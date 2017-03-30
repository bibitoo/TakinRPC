package com.takin.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.takin.rpc.client.loadbalance.ConsistentHashLoadBalance;
import com.takin.rpc.client.loadbalance.LoadBalance;
import com.takin.rpc.remoting.netty5.RemotingProtocol;

/**
 * ProxyStandard
 *
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ProxyStandard implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyStandard.class);

    private Class<?> interfaceClass;
    private String lookup = "";
    private String serviceName = "";
    private LoadBalance balance = new ConsistentHashLoadBalance();

    /** 
     * 
     * @param interfaceClass 接口类 
     * @param serviceName 服务名
     * @param lookup 接口实现类
     * @param balance 使用哪一个负载均衡算法
     */
    public ProxyStandard(Class<?> interfaceClass, String serviceName, String lookup, LoadBalance balance) {
        this.interfaceClass = interfaceClass;
        this.serviceName = serviceName;
        this.lookup = lookup;
        if (balance != null) {
            this.balance = balance;
        }
    }

    /**
     * 
     * args 参数
     * method 方法
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Type[] typeAry = method.getGenericParameterTypes();//ex:java.util.Map<java.lang.String, java.lang.String>
        Class<?>[] clsAry = method.getParameterTypes();//ex:java.util.Map
        if (args == null) {
            args = new Object[0];
        }
        if (args.length != typeAry.length) {
            throw new Exception("argument count error!");
        }
        boolean syn = true;
        RemotingProtocol<?> message = new RemotingProtocol<>();
        message.setClazz(interfaceClass);
        message.setMethod(method.getName());
        message.setArgs(args);
        message.setmParamsTypes(clsAry);
        String address = balance.select(NamingFactory.getInstance().getConfigAddr(serviceName), "");

        logger.info(String.format("request: %s", JSONObject.toJSONString(message)));
        message = RemotingNettyClient.getInstance().invokeSync(address, message, 2000);
        //                logger.info(String.format("response: %s", JSONObject.toJSONString(message)));
        System.out.println("return:" + message.getResultJson());
        return message.getResultJson();
    }
}
