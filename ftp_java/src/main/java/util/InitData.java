package util;

import cache.EhCacheUtil;

import java.io.*;
import java.util.Properties;

import static cache.EhCacheUtil.user_prefix;

/**
 * Java解析.ini文件
 *
 * @author Administrator
 */
public class InitData {


    public static void init() throws Exception {
        InputStream in = new FileInputStream(new File("../config/account.ini"));
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        Properties props = new Properties();
        props.load(br);
        for (Object s : props.keySet()) {
            EhCacheUtil.put(user_prefix + s.toString(), props.getProperty(s.toString()));
        }
    }
}