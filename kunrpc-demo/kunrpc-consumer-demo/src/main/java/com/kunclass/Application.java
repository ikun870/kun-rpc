package com.kunclass;

import com.kunapi.HelloKunrpc;


import java.lang.ref.Reference;

public class Application {
    public static void main(String[] args) {
        // �뾡һ�а취��ȡ�������ʹ��referenceConfig���з�װ
        //referenceConfigһ�������ɴ������ķ�����get()
        ReferenceConfig<HelloKunrpc> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(HelloKunrpc.class);


        //��������ʲô
        //1.����ע������ 2.��ȡ�����б� 3.ѡ��һ�����񲢽������� 4.��������Я��һЩ��Ϣ���ӿ����������б����������֣�����ý��
        KunrpcBootstrap.getInstance()
                .application("first-kunrpc-consumer")
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .reference(referenceConfig);

        // ��ȡһ���������
        HelloKunrpc helloKunrpc = referenceConfig.get();
        helloKunrpc.sayHi("kunrpc");

    }
}
