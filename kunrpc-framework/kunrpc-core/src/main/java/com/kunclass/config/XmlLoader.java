package com.kunclass.config;

import com.kunclass.Compress.Compressor;
import com.kunclass.Compress.CompressorFactory;
import com.kunclass.Serialize.Serializer;
import com.kunclass.Serialize.SerializerFactory;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Slf4j
public class XmlLoader {

    /**
     * 从配置文件读取配置消息
     * @param configuration
     */
    public void loadFromXml(Configuration configuration) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // 禁用DTD校验：可以通过调用setValidating(false)方法来禁用DTD校验。
            factory.setValidating(false);
            // 禁用外部实体解析：可以通过调用setFeature(String name, boolean value)方法并将“http://apache.org/xml/features/nonvalidating/load-external-dtd”设置为“false”来禁用外部实体解析。
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            //1.创建Document
            DocumentBuilder builder = factory.newDocumentBuilder();
            //在这里输出当前的路径
            // 获取类加载器
            ClassLoader classLoader = getClass().getClassLoader();

            Document document = builder.parse(classLoader.getResourceAsStream("kunrpc.xml"));//采用相对路径
            //kunrpc-demo/kunrpc-provider-demo/src/main/resources/kunrpc.xml
            //kunrpc-demo/kunrpc-springboot-provider/src/main/resources/kunrpc.xml
            //2.获取根节点
            Element root = document.getDocumentElement();
            // 获取应用名称（保持不变）
            String appName = root.getElementsByTagName("appName").item(0).getTextContent();
            if(!appName.isEmpty()) {
                configuration.setAppName(appName);
            }
            else log.info("appName is null, use default appName:"+appName+"!");

            // 获取端口号（保持不变）
            String port = root.getElementsByTagName("port").item(0).getTextContent();
            if(port != null) {
                configuration.setPort(Integer.parseInt(port));
            } else log.info("port is null, use default port:"+port+"!");


            // 获取注册中心（修改为属性获取）
            Element registryElement = (Element) root.getElementsByTagName("registry").item(0);
            String registryUrl = registryElement.getAttribute("url");
            if(!registryUrl.isEmpty()) {
                configuration.setRegistryConfig(parseConfig(
                        "com.kunclass.discovery.RegistryConfig",
                        new Class[]{String.class},
                        registryUrl
                ));
            }
            else log.info("registry is null, use default registry:"+registryUrl+"!");

            // 序列化协议（支持两种配置方式）
            Element serializeElement = (Element) root.getElementsByTagName("serializeType").item(0);
            String serializeType = serializeElement.getAttribute("type");
            if (serializeType.isEmpty()) {
                if(serializeElement.getElementsByTagName("serializer").getLength() == 0) {
                    log.info("serializeType is null, use default serializeType:" + serializeType + "!");
                }
                else {
                    //这种情况将serializerWrapper添加到序列化工厂中就ok了
                    SerializerFactory.addSerializer(parseSerializerOrCompressor("serializer", root));
                }
            }else  configuration.setSerializeType(serializeType);


            // 压缩协议（支持两种配置方式）
            Element compressElement = (Element) root.getElementsByTagName("compressType").item(0);
            String compressType = compressElement.getAttribute("type");
            if(compressType.isEmpty()) {
                if(compressElement.getElementsByTagName("compressor").getLength() == 0) {
                    log.info("compressType is null, use default compressType:" + compressType + "!");
                }
                else {
                    //这种情况将compressWrapper添加到压缩工厂中就ok了
                    CompressorFactory.addCompressor(parseSerializerOrCompressor("compressor", root));
                }
            } else configuration.setCompressType(compressType);

            // 负载均衡器（支持两种配置方式）
            Element lbElement = (Element) root.getElementsByTagName("loadBalancer").item(0);
            String loadBalancerClass = lbElement.getAttribute("class");
            if(loadBalancerClass.isEmpty()) {
                if (lbElement.getElementsByTagName("loadbalancer").getLength() == 0) {
                    log.info("loadBalancer is null, use default loadBalancer:" + configuration.getLoadBalancer().getClass().getName() + "!");
                } else {
                    //TODO LoadBalancer这里有问题，xml请使用Type赋值
                    loadBalancerClass = ((Element) root.getElementsByTagName("loadbalancer").item(0)).getAttribute("class");
                    configuration.setLoadBalancer(parseConfig(loadBalancerClass,null));
                }
            } else configuration.setLoadBalancer(parseConfig(loadBalancerClass,null));

            // ID生成器（修复属性获取）
            Element idGeneratorElement = (Element) root.getElementsByTagName("idGenerator").item(0);
            String idGeneratorClass = idGeneratorElement.getAttribute("class");
            String dataCenterId = idGeneratorElement.getAttribute("dataCenterId");
            String machineId = idGeneratorElement.getAttribute("machineId");
            if(idGeneratorClass.isEmpty() || dataCenterId.isEmpty() || machineId.isEmpty()) {
                log.info("idGenerator is null, use default idGenerator:" + configuration.getIdGenerator().getClass().getName() + "!");
            } else configuration.setIdGenerator(parseConfig(idGeneratorClass, new Class[]{long.class, long.class}, Long.parseLong(dataCenterId), Long.parseLong(machineId)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.info("读取xml配置文件失败,使用默认配置！");
        }
    }

    /**
     * 将xml配置中的一些String转换为对象
     */
    private <T> T parseConfig(String config,Class<?>[] parameterTypes,Object... parameters) {
        //将xml配置中的一些String转换为对象
        //反射获取一个对象，这个对象可以是RegistryConfig、ProtocolConfig、LoadBalancer
        //根据配置的类名获取类对象
        Class<T> clazz = null;
        try {
            clazz = (Class<T>) Class.forName(config);
            //获取类的构造方法
            if(parameterTypes == null) {
                return clazz.getConstructor().newInstance();
            }
            else {
                return clazz.getConstructor(parameterTypes).newInstance(parameters);
            }
            //获取构造方法
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            log.info("xml文件解析失败，使用默认配置！");
        }
        return null;

    }

    /**
     * 解析序列化器和压缩器
     * @param s
     * @param root
     * @return
     * @param <T>
     */
    private <T> ObjectWrapper<T> parseSerializerOrCompressor(String s, Element root) {
        Element element = (Element) root.getElementsByTagName(s).item(0);
        String code = element.getAttribute("code");
        String name = element.getAttribute("name");
        String className = element.getAttribute("class");
        T object = parseConfig(className, null);
        return new ObjectWrapper<>(Byte.parseByte(code), name, object);
    }
}
