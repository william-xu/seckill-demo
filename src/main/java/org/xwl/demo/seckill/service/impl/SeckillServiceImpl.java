package org.xwl.demo.seckill.service.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.xwl.demo.seckill.config.RabbitMQProducer;
import org.xwl.demo.seckill.config.ZookeeperConfig;
import org.xwl.demo.seckill.config.ZookeeperCuratorClientManager;
import org.xwl.demo.seckill.constant.RedisKeyPrefix;
import org.xwl.demo.seckill.constant.SeckillStateEnum;
import org.xwl.demo.seckill.dao.SeckillDAO;
import org.xwl.demo.seckill.dao.SuccessKilledDAO;
import org.xwl.demo.seckill.dto.Exposer;
import org.xwl.demo.seckill.dto.SeckillExecution;
import org.xwl.demo.seckill.dto.SeckillMsgBody;
import org.xwl.demo.seckill.entity.Seckill;
import org.xwl.demo.seckill.entity.SuccessKilled;
import org.xwl.demo.seckill.exception.SeckillException;
import org.xwl.demo.seckill.service.AccessLimitService;
import org.xwl.demo.seckill.service.SeckillService;
import org.xwl.demo.seckill.utils.RedisUtil;

import com.alibaba.fastjson.JSON;

/**
 * @author liushaoming
 */
@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //md5盐值字符串,用于混淆MD5
    private final String salt = "aksksks*&&^%%aaaa&^^%%*";

    //注入Service依赖
    @Autowired
    private SeckillDAO seckillDAO;
    @Autowired
    private SuccessKilledDAO successKilledDAO;
    @Autowired
    private RedisUtil redisDAO;
    @Autowired
    private AccessLimitService accessLimitService;

    @Autowired
    private RabbitMQProducer mqProducer;

    @Resource
    private ZookeeperCuratorClientManager curatorClientManager;

    @Autowired
    private RedisConnectionFactory redisConnFactory;

    @Autowired
    private ZookeeperConfig zkConfig;

    @Autowired
    private RedissonClient redissonClient;
    
    private Object sharedObj = new Object();

    @Override
    public List<Seckill> getSeckillList() {
        return seckillDAO.queryAll(0, 10);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDAO.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        // 优化点:缓存优化:超时的基础上维护一致性
        //1.访问Redis
        Seckill seckill = redisDAO.getSeckill(seckillId);
        if (seckill == null) {
            //2.访问数据库
            seckill = seckillDAO.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                //3.存入Redis
                redisDAO.putSeckill(seckill);
            }
        }

        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        //系统当前时间
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(),
                    endTime.getTime());
        }
        //转化特定字符串的过程，不可逆
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    @Override
    /**
     * 执行秒杀
     */
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException {
        if (accessLimitService.tryAcquireSeckill()) {   // 如果没有被限流器限制，则执行秒杀处理
//            return handleSeckill(seckillId, userPhone, md5);
            return handleSeckillAsync(seckillId, userPhone, md5);
        } else {    //如果被限流器限制，直接抛出访问限制的异常
            logger.info("--->ACCESS_LIMITED-->seckillId={},userPhone={}", seckillId, userPhone);
            throw new SeckillException(SeckillStateEnum.ACCESS_LIMIT);
        }
    }

    /**
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @TODO 先在redis里处理，然后发送到mq，最后减库存到数据库
     */
    private SeckillExecution handleSeckillAsync(long seckillId, long userPhone, String md5)
            throws SeckillException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            logger.info("seckill_DATA_REWRITE!!!. seckillId={},userPhone={}", seckillId, userPhone);
            throw new SeckillException(SeckillStateEnum.DATA_REWRITE);
        }
        //库存前缀+商品id
        String inventoryKey = RedisKeyPrefix.SECKILL_INVENTORY + seckillId;
        //购买用户前缀+商品ID        
        String boughtKey = RedisKeyPrefix.BOUGHT_USERS + seckillId;
        
        //如果手机号码已经存在则是重复秒
        if (redisConnFactory.getConnection().sIsMember(boughtKey.getBytes(), String.valueOf(userPhone).getBytes())) {
            //重复秒杀
            logger.info("SECKILL_REPEATED. seckillId={},userPhone={}", seckillId, userPhone);
            throw new SeckillException(SeckillStateEnum.REPEAT_KILL);
        } else {
        	//通过Curator创建Zookeeper分布式锁
//            CuratorFramework client = curatorClientManager.getClient();
//            if (client.getState().compareTo(CuratorFrameworkState.STARTED)!=0) {
//            	client.start();
//            }
//            InterProcessLock lock = new InterProcessMutex(client, zkConfig.getLockRoot());

            RLock rLock = redissonClient.getLock("DIST_REDISSON_LOCK");

            boolean lockSuccess = false;
            try {
            	//获取分布式锁
//                lockSuccess = lock.acquire(zkConfig.getLockAcquireTimeout(), TimeUnit.MILLISECONDS);
                try {
                	lockSuccess = rLock.tryLock(zkConfig.getLockAcquireTimeout(), zkConfig.getSessionTimeout(), TimeUnit.MILLISECONDS);	
                } catch (Exception e) {
                	//获取分布式锁失败
                    logger.error(e.getMessage(), e);
                    logger.info("SECKILL_DISTLOCK_ACQUIRE_EXCEPTION---seckillId={},userPhone={}", seckillId, userPhone);

                    throw new SeckillException(SeckillStateEnum.DISTLOCK_ACQUIRE_FAILED);
                }

                long threadId = Thread.currentThread().getId();
                logger.info("threadId={}, lock_success={}",
                        new Object[]{threadId, lockSuccess});
                if(lockSuccess) {
                	//再次判断是否重复秒杀
                	if (redisConnFactory.getConnection().sIsMember(boughtKey.getBytes(), String.valueOf(userPhone).getBytes())) {
                        //重复秒杀
                        logger.info("SECKILL_REPEATED. seckillId={},userPhone={}", seckillId, userPhone);
                        throw new SeckillException(SeckillStateEnum.REPEAT_KILL);
                    }
                    //减库存
                    handleInRedis(userPhone, inventoryKey, boughtKey);
                }else {
                	throw new SeckillException(SeckillStateEnum.DISTLOCK_ACQUIRE_FAILED);
                }

                // 秒杀成功，后面异步更新到数据库中
                // 发送消息到消息队列
                SeckillMsgBody msgBody = new SeckillMsgBody();
                msgBody.setSeckillId(seckillId);
                msgBody.setUserPhone(userPhone);
                mqProducer.send(JSON.toJSONString(msgBody));

                // 立即返回给客户端，说明秒杀成功了
                SuccessKilled successKilled = new SuccessKilled();
                successKilled.setUserPhone(userPhone);
                successKilled.setSeckillId(seckillId);
                successKilled.setState(SeckillStateEnum.SUCCESS.getState());
                logger.info("SECKILL_SUCCESS>>>seckillId={},userPhone={}", seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);               
            }finally {
                long threadId = Thread.currentThread().getId();
            	try {
//                    lock.release();
            		if(lockSuccess)
            		rLock.unlock();
                    logger.info("threadId={}, lock_released={}",
                            new Object[]{threadId, lockSuccess});
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
//                if (client.getState().compareTo(CuratorFrameworkState.STARTED)==0) {
//                    CloseableUtils.closeQuietly(client);
//                }
            }

        }
    }


    private void handleInRedis(long userPhone, String inventoryKey
            , String boughtKey) throws SeckillException {
        String inventoryStr = new String(redisConnFactory.getConnection().get(inventoryKey.getBytes()));
        int inventory = Integer.valueOf(inventoryStr);
        if (inventory <= 0) {
            throw new SeckillException(SeckillStateEnum.SOLD_OUT);
        }
        redisConnFactory.getConnection().decr(inventoryKey.getBytes());
        redisConnFactory.getConnection().sAdd(boughtKey.getBytes(), String.valueOf(userPhone).getBytes());
        logger.info("handleInRedis_done");
    }

    /**
     * 直接在数据库里减库存
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     */
    @Deprecated // 通过redis+mq的形式更能应对高并发，这里的传统方法，舍弃掉
//    #handleSeckillAsync(seckillId, userPhone, md5)
    /**
     * 请使用 {@link SeckillServiceImpl#handleSeckillAsync(long, long, String)}   }
     */
    private SeckillExecution handleSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            logger.info("seckill_DATA_REWRITE!!!. seckillId={},userPhone={}", seckillId, userPhone);
            throw new SeckillException(SeckillStateEnum.DATA_REWRITE);
        }
        return doUpdateStock(seckillId, userPhone);
    }

    /**
     * 先插入秒杀记录再减库存
     */
    @Override
    @Transactional
    public SeckillExecution doUpdateStock(long seckillId, long userPhone)
            throws SeckillException {
        //执行秒杀逻辑:减库存 + 记录购买行为
        Date nowTime = new Date();
        try {
            //插入秒杀记录(记录购买行为)
            //这处， seckill_record的id等于这个特定id的行被启用了行锁,   但是其他的事务可以insert另外一行， 不会阻止其他事务里对这个表的insert操作
            int insertCount = successKilledDAO.insertSuccessKilled(seckillId, userPhone, nowTime);
            //唯一:seckillId,userPhone
            if (insertCount <= 0) {
                //重复秒杀
                logger.info("seckill REPEATED. seckillId={},userPhone={}", seckillId, userPhone);
                throw new SeckillException(SeckillStateEnum.REPEAT_KILL);
            } else {
                //减库存,热点商品竞争
                // reduceNumber是update操作，开启作用在表seckill上的行锁
                Seckill currentSeckill = seckillDAO.queryById(seckillId);
                boolean validTime = false;
                if (currentSeckill != null) {
                    long nowStamp = nowTime.getTime();
                    if (nowStamp > currentSeckill.getStartTime().getTime() && nowStamp < currentSeckill.getEndTime().getTime()
                            && currentSeckill.getInventory() > 0 && currentSeckill.getVersion() > -1) {
                        validTime = true;
                    }
                }

                if (validTime) {
                    long oldVersion = currentSeckill.getVersion();
                    // update操作开始，表seckill的seckill_id等于seckillId的行被启用了行锁,   其他的事务无法update这一行， 可以update其他行
                    int updateCount = seckillDAO.reduceInventory(seckillId, oldVersion, oldVersion + 1);
                    if (updateCount <= 0) {
                        //没有更新到记录，秒杀结束,rollback
                        logger.info("seckill_DATABASE_CONCURRENCY_ERROR!!!. seckillId={},userPhone={}", seckillId, userPhone);
                        throw new SeckillException(SeckillStateEnum.DB_CONCURRENCY_ERROR);
                    } else {
                        //秒杀成功 commit
                        SuccessKilled successKilled = successKilledDAO.queryByIdWithSeckill(seckillId, userPhone);
                        logger.info("seckill SUCCESS->>>. seckillId={},userPhone={}", seckillId, userPhone);
                        return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                        //return后，事务结束，关闭作用在表seckill上的行锁
                        // update结束，行锁被取消  。reduceInventory()被执行前后数据行被锁定, 其他的事务无法写这一行。
                    }
                } else {
                    logger.info("seckill_END. seckillId={},userPhone={}", seckillId, userPhone);
                    throw new SeckillException(SeckillStateEnum.END);
                }
            }
        } catch (SeckillException e1) {
            throw e1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            //  所有编译期异常 转化为运行期异常
            throw new SeckillException(SeckillStateEnum.INNER_ERROR);
        }
    }
}
