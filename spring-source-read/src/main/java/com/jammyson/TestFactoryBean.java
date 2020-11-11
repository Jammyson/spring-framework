package com.jammyson;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author Y.H.Zhou - zhouyuhang@deepexi.com
 * @since 2020/9/8.
 * <p> </p>
 */
@Component
public class TestFactoryBean implements FactoryBean<TestFactoryBean.TestBean> {

	@Override
	public TestBean getObject() throws Exception {
		return new TestBean();
	}

	@Override
	public Class<?> getObjectType() {
		/**
		 * 如果getObjectType和getObject的class对象不一致，则会报错
		 * 		No qualifying bean of type 'com.jammyson.TestFactoryBean.TestBean' available
		 */
		return TestBean.class;
	}

	public static class TestBean {}
}
