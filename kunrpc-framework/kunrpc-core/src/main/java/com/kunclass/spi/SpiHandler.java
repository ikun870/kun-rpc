package com.kunclass.spi;

import com.kunclass.config.ObjectWrapper;
import com.kunclass.exceptions.SpiException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现一个简易版本的SPI
 */
@Slf4j
public class SpiHandler {
    //定义一个basePath
    private static final String BASE_PATH = "META-INF/services/";

    //先定义一个缓存，保存spi中相关的原始内容

    private static Map<String, List<String>> SPI_CONTENT = new ConcurrentHashMap<>(8);

    //定义一个缓存，保存对于某一个接口的spi中相关的实现类
    private static final Map<Class<?>, List<ObjectWrapper<?>>> SPI_IMPLEMENT = new ConcurrentHashMap<>(32);

    //在加载当前类时就执行，将spi中的内容加载到SPI_CONTENT，避免了运行时频繁执行TO
    static {
        //加载当前工程和jar包中的所有spi文件

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(BASE_PATH);
        if (resource != null) {
            //将url中的中文字符转换为utf-8
            String path = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);
            File[] files = new File(path).listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    //获取文件中的内容
                    List<String> lines = readImplObject(file);
                    SPI_CONTENT.put(fileName, lines);
                }
            }
        }

    }

    /**
     * 读取文件中的内容，按行读取所有的实现类
     * @param file 文件对象
     * @return 具体的配置实现类List
     */
    private static List<String> readImplObject(File file) {

        try {
            //读取文件中的内容
            return java.nio.file.Files.readAllLines(file.toPath());
        } catch (java.io.IOException e) {
            log.error("读取SPI文件时，发生异常！", e);
        }
        return null;

    }

    /**
     * 建立SPI_IMPLEMENT缓存
     * @param clazz 接口的Class对象
     * @param <T> 接口的类型
     */
    private static <T> void buildCache(Class<T> clazz) {
        SPI_IMPLEMENT.putIfAbsent(clazz, new ArrayList<>());
        String name = clazz.getName();
        //获取SPI_CONTENT中的内容
        List<String> implObjects_String = SPI_CONTENT.get(name);
        //实例化所有的实现类
        if (implObjects_String != null) {
            for (String implObject_String : implObjects_String) {
                try {
                    //1-ConsistentHashBalancer-com.kunclass.loadBalancer.impl.ConsistentHashBalancer
                    //先处理原始字符串
                    String[] Code_Type_Name = implObject_String.split("-");
                    if (Code_Type_Name.length != 3) {
                        throw new SpiException("SPI文件中的实现类格式不正确---" + implObject_String);
                    }
                    Byte Code = Byte.parseByte(Code_Type_Name[0]);
                    String Type = Code_Type_Name[1];
                    String implObjectName = Code_Type_Name[2];

                    //获取实现类的Class对象
                    Class<T> implClass = (Class<T>) Class.forName(implObjectName);
                    //判断是否是clazz的子类
                    if (clazz.isAssignableFrom(implClass)) {
                        //实例化实现类
                        T implObject = implClass.getConstructor().newInstance();
                        //将实现类封装成ObjectWrapper对象
                        ObjectWrapper<T> objectWrapper = new ObjectWrapper<>(Code, Type, implObject);
                        //将实现类放入缓存中
                        SPI_IMPLEMENT.get(clazz).add(objectWrapper);
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException |
                         InvocationTargetException e) {
                    log.error("获取SPI实现类{}时，发生异常！",implObject_String, e);
                }
            }
        }

    }

    /**
     * 获取实现类,第一个
     * @param clazz
     * @return
     * @param <T>
     */

    public synchronized static <T> ObjectWrapper<T> get(Class<T> clazz) {
        //尝试获取缓存中的实现类
        List<ObjectWrapper<?>> implObjects = SPI_IMPLEMENT.get(clazz);
        //如果缓存中有实现类，则直接返回
        if (implObjects != null && !implObjects.isEmpty()) {
            //返回第一个实现类
            return (ObjectWrapper<T>) implObjects.get(0);
        }

        //否则建立实现类的缓存
        buildCache(clazz);

        if (SPI_IMPLEMENT.get(clazz) == null || SPI_IMPLEMENT.get(clazz).isEmpty()) {
            log.error("没有找到实现类！");
            return null;
        }
        return (ObjectWrapper<T>) SPI_IMPLEMENT.get(clazz).get(0);
    }

    /**
     * 获取实现类,所有
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public synchronized static <T> List<ObjectWrapper<T>> getList(Class<T> clazz) {
        //获取缓存中的实现类
        List<ObjectWrapper<?>> implObjects = SPI_IMPLEMENT.get(clazz);
        //如果缓存中有实现类，则直接返回
        if (implObjects != null && !implObjects.isEmpty()) {
            List<ObjectWrapper<T>> res = implObjects.stream().map(wrapper -> {
                try {
                    return (ObjectWrapper<T>) wrapper;
                } catch (ClassCastException e) {
                    log.error("类型转换异常！", e);
                    return null;
                }
            }).toList();
            //返回第一个实现类
            return  res;
        }

        //否则建立实现类的缓存
        buildCache(clazz);
        if (SPI_IMPLEMENT.get(clazz) == null || SPI_IMPLEMENT.get(clazz).isEmpty()) {
            log.error("没有找到实现类！");
            return null;
        }
        return  SPI_IMPLEMENT.get(clazz).stream().map(wrapper -> {
            try {
                return (ObjectWrapper<T>) wrapper;
            } catch (ClassCastException e) {
                log.error("类型转换异常！", e);
                return null;
            }
        }).toList();
    }


    public static void main(String[] args) {
        Map<Integer, List<String>> map = new HashMap<>();
        ArrayList<String> objects = new ArrayList<>();
        List<String> a = map.get(1);

        objects.add("1");
        map.put(1, objects);
        map.get(1).add("2");
        System.out.println(a.toString());
    }
}
