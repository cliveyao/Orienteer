package org.orienteer.core.dao.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.orienteer.core.dao.IMethodHandler;
import org.orienteer.core.dao.StackInvocationHandler;

/**
 * Implementation of equals method which perform unwrapping prior to calling equal
 * @param <T> type of target/delegate object
 */
public class EqualsMethodHandler<T> implements IMethodHandler<T> {

	@Override
	public ResultHolder handle(T target, Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getName().equals("equals") && args.length==1) {
			Object other = args[0];
			if(other==null) return new ResultHolder(false);
			if(Proxy.isProxyClass(other.getClass())) {
				if(proxy.getClass().equals(other.getClass())) {
					InvocationHandler handler = Proxy.getInvocationHandler(other);
					if(handler instanceof StackInvocationHandler) {
						Object otherTarget = ((StackInvocationHandler<?>)handler).getTarget();
						return new ResultHolder(target.equals(otherTarget));
					}
				}
				return new ResultHolder(false);
			}
		}
		return null;
	}

}
