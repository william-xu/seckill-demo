/*
Navicat MariaDB Data Transfer

Source Server         : linux-cn-seckill
Source Server Version : 50560
Source Host           : localhost:3306
Source Database       : seckill

Target Server Type    : MariaDB
Target Server Version : 50560
File Encoding         : 65001

Date: 2019-01-22 16:06:53
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for seckill
-- ----------------------------
DROP TABLE IF EXISTS `seckill`;
CREATE TABLE `seckill` (
  `seckill_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
  `name` varchar(120) CHARACTER SET utf8 NOT NULL COMMENT '商品名称',
  `inventory` int(11) NOT NULL COMMENT '库存数量',
  `start_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '秒杀开启时间',
  `end_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '秒杀结束时间',
  `create_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '创建时间',
  `version` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`seckill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀库存表';

-- ----------------------------
-- Records of seckill
-- ----------------------------
INSERT INTO `seckill` VALUES ('1000', '商品0', '9937', '2019-01-15 11:00:00', '2019-10-25 00:00:00', '2019-01-13 21:28:31', '15');
INSERT INTO `seckill` VALUES ('1001', '商品1', '181', '2019-01-16 11:00:00', '2019-10-25 00:00:00', '2019-01-13 21:28:31', '7');
INSERT INTO `seckill` VALUES ('1002', '商品2', '293', '2019-01-17 11:00:00', '2019-10-25 00:00:00', '2019-01-13 21:28:31', '5');
INSERT INTO `seckill` VALUES ('1003', '商品3', '398', '2019-11-30 11:00:00', '2019-12-01 00:00:00', '2019-01-13 21:28:31', '0');

/*
Navicat MySQL Data Transfer

Source Server         : localhost-mysql-seckill
Source Server Version : 50721
Source Host           : localhost:3306
Source Database       : seckill

Target Server Type    : MYSQL
Target Server Version : 50721
File Encoding         : 65001

Date: 2019-01-14 17:47:44
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for success_killed
-- ----------------------------
DROP TABLE IF EXISTS `success_killed`;
CREATE TABLE `success_killed` (
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀商品id',
  `user_phone` bigint(20) NOT NULL COMMENT '用户手机号',
  `state` tinyint(4) NOT NULL DEFAULT '-1' COMMENT '状态标示:-1:无效 0:成功 1:已付款 2:已发货',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`seckill_id`,`user_phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';

-- ----------------------------
-- Records of success_killed
-- ----------------------------
INSERT INTO `success_killed` VALUES ('1000', '13600000000', '0', '2019-01-13 21:47:42');
INSERT INTO `success_killed` VALUES ('1000', '13700000000', '0', '2019-01-14 17:24:13');

