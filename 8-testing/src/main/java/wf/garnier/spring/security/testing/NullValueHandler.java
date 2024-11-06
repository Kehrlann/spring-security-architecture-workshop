package wf.garnier.spring.security.testing;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.springframework.stereotype.Component;

@Component
class NullValueHandler implements MethodAuthorizationDeniedHandler {

	@Override
	public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
		// If the access is denied, return null.
		// You have access to the MethodInvocation, so you could apply specific return
		// values based on the parameters passed to the original method.
		return null;
	}

}
