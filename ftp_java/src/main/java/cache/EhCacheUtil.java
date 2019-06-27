package cache;

import lombok.extern.java.Log;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import java.io.Serializable;
import java.util.Collection;

/**
 * EhCacheUtil
 * <p>
 * 使用说明：
 * 1、
 * 在需要缓存的 DAO的 *mapper.xml中添加
 * 以下两个<cache>标签都可以,第一个可以输出日志,第二个不输出日志
 * <cache type="org.mybatis.caches.ehcache.LoggingEhcache"/>
 * <cache type="org.mybatis.caches.ehcache.EhcacheCache"/>
 * 也可在单条中配置 ：flushCache    useCache 的属性
 * 2、
 * 首先在ehcache.xml中配置缓存策略，即添加一组cache。
 * 参考下方代码
 * 3、注解
 */
@Log
public class EhCacheUtil {

    public static String file_prefix = "ftp:file:";
    public static String md5_prefix = "ftp:md5:";
    public static final String user_prefix = "ftp:user:";
    public static final String user_online = "ftp:online:";
    public static final String name_prefix = "ftp:name:";

    public static final String obj_file_prefix = "ftp:obj:file:";
    public static final String obj_out_prefix = "ftp:obj:out:";
    public static final String obj_check_prefix = "ftp:obj:check:";

    private static CacheManager manager = null;

    static {
        try {
            manager = CacheManager.create(EhCacheUtil.class.getClassLoader().getResourceAsStream("ehcache.xml"));
        } catch (CacheException e) {
            e.printStackTrace();
            log.info("获取ehcache.xml失败");
        }
    }

    /**
     * 存入
     *
     * @param <T>   the type parameter
     * @param key   键
     * @param value 值
     */
    public static <T> void put(String key, T value) {
        Cache cache = checkCache("ftp");
        Element e = new Element(key, value);
        e.setEternal(true);
        cache.put(e);
        log.info(cache.get(key).toString());
//        cache.flush();
    }

    /**
     * 存入
     *
     * @param <T>       the type parameter
     * @param cacheName the cache name
     * @param key       键
     * @param value     值
     */
    public static <T> void put(String cacheName, String key, T value) {
        Cache cache = checkCache(cacheName);
        Element e = new Element(key, value);
        cache.put(e);
        cache.flush();
    }

    /**
     * 存入 并设置元素是否永恒保存
     *
     * @param <T>       the type parameter
     * @param cacheName the cache name
     * @param key       键
     * @param value     值
     * @param eternal   对象是否永久有效，一但设置了，timeout将不起作用
     */
    public static <T extends Serializable> void put(String cacheName, String key, T value, boolean eternal) {
        Cache cache = checkCache(cacheName);
        Element element = new Element(key, value);
        element.setEternal(eternal);
        cache.put(element);
        cache.flush();
    }

    /**
     * 存入
     *
     * @param <T>               the type parameter
     * @param cacheName         the cache name
     * @param key               键
     * @param value             值
     * @param timeToLiveSeconds 最大存活时间
     * @param timeToIdleSeconds 最大访问间隔时间
     */
    public static <T extends Serializable> void put(String cacheName, String key, T value, int timeToLiveSeconds, int timeToIdleSeconds) {
        Cache cache = checkCache(cacheName);
        Element element = new Element(key, value);
        element.setTimeToLive(timeToLiveSeconds);
        element.setTimeToIdle(timeToIdleSeconds);
        cache.put(element);
        cache.flush();
    }

    /**
     * Get object.
     *
     * @param key the key
     * @return the object
     */
    public static Object get(String key) {
        Cache cache = checkCache("ftp");
        Element e = cache.get(key);
        if (e != null) {
            return e.getObjectValue();
        }
        return null;
    }

    /**
     * Get object.
     *
     * @param cacheName the cache name
     * @param key       the key
     * @return the object
     */
    public static Object get(String cacheName, String key) {
        Cache cache = checkCache(cacheName);
        Element e = cache.get(key);
        if (e != null) {
            return e.getObjectValue();
        }
        return null;
    }

    /**
     * Remove.
     *
     * @param key the key
     */
    public static void remove(String key) {
        Cache cache = checkCache("ftp");
        cache.remove(key);
    }

    /**
     * Remove.
     *
     * @param cacheName the cache name
     * @param key       the key
     */
    public static void remove(String cacheName, String key) {
        Cache cache = checkCache(cacheName);
        cache.remove(key);
    }

    /**
     * Remove all.
     *
     * @param cacheName the cache name
     * @param keys      the keys
     */
    public static void removeAll(String cacheName, Collection<String> keys) {
        Cache cache = checkCache(cacheName);
        cache.removeAll(keys);
    }

    /**
     * Clears the contents of all caches in the CacheManager, but without
     * removing any caches.
     * <p/>
     * This method is not synchronized. It only guarantees to clear those elements in a cache at
     * the time that the
     */
    public static void clearAll() {
        manager.clearAll();
    }

    private static Cache checkCache(String cacheName) {
        Cache cache = manager.getCache(cacheName);
        if (null == cache) {
            throw new IllegalArgumentException("name=[" + cacheName + "],不存在对应的缓存组,请查看ehcache.xml");
        }
        return cache;
    }

}