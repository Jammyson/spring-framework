package com.jammyson;

import org.springframework.stereotype.Component;

/**
 * @author Y.H.Zhou - zhouyuhang@deepexi.com
 * @since 2020/11/10.
 * <p> 用于测试如果Bean实现了接口并重写了方法,那么Bean将会怎样被创建 </p>
 * 结论:不论是实现了接口的Bean,还是普通的Bean,它们都是使用反射的形式创建对象.并没有使用
 * CGLIB等技术.
 */
@Component
public class TestBeanImplInterface implements IInterface {

	@Override
	public void doSomething() {
		// do nothing
	}
}
