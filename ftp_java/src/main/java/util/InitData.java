package util;

import cache.JedisUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static cache.JedisUtil.user_prefix;

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
            Objects.requireNonNull(JedisUtil.getJedis())
                    .set(user_prefix + s.toString(), props.getProperty(s.toString()));
        }
    }
}