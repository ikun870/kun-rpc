package com.kunclass;

import com.kunclass.annotation.KunrpcService;
import com.kunclass.proxy.KunrpcProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class KunrpcProxyBeanPostProcessor implements BeanPostProcessor {

    //会拦截所有的bean的创建，会在每一个bean初始后被调用
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
    //想办法给他生成一个代理
        Field[] declaredFields = bean.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            //判断是否有KunrpcService注解
            KunrpcService kunrpcService = declaredField.getAnnotation(KunrpcService.class);
            if (kunrpcService != null) {
                //生成代理对象
                Object proxy = KunrpcProxyFactory.getProxy(declaredField.getType());//获取接口类型
                declaredField.setAccessible(true);//设置为可访问
                try {
                    declaredField.set(bean,proxy);//设置代理对象
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }
}
