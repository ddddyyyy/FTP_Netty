package cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 这是一个jedis连接池工具类，以及一些jedis基本应用。
 *
 * @author lin
 * @date 2017/10/12
 */
public class JedisUtil {

    public static String file_prefix = "ftp:file:";
    public static String md5_prefix = "ftp:md5:";
    public static final String user_prefix = "ftp:user:";
    public static final String user_online = "ftp:online:";

    private static JedisPool jedisPool;

    /**
     * 初始化Redis连接池
     */
    public static void init() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();

            //可用连接实例的最大数目，默认值为8；
            //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
            int MAX_ACTIVE = 1024;
            config.setMaxTotal(MAX_ACTIVE);
            //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值8。
            int MAX_IDLE = 200;
            config.setMaxIdle(MAX_IDLE);
            //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
            int MAX_WAIT = 10000;
            config.setMaxWaitMillis(MAX_WAIT);
            //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
            config.setTestOnBorrow(true);
//            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT);
            //需要认证
            //Redis服务器IP
            String ADDR = "127.0.0.1";
            //Redis的端口号
            int PORT = 6379;
            //访问密码
            String AUTH = "1234567890***";
            int TIMEOUT = 10000;
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Jedis实例
     */
    public synchronized static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                return jedisPool.getResource();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 释放jedis资源
     */
    public static void close() {
        jedisPool.close();
    }

}
