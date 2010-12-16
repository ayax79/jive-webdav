package com.syncapse.jive.milton

import java.lang.String
import com.bradmcevoy.http.{Resource, Auth, Request, SecurityManager}
import com.bradmcevoy.http.Request.Method
import com.syncapse.jive.auth.AuthenticationManagerComponent
import com.bradmcevoy.http.http11.auth.DigestResponse
import com.syncapse.jive.Loggable
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken
import org.acegisecurity.AuthenticationException

class JiveMiltonSecurityManager extends SecurityManager with AuthenticationManagerComponent with Loggable {
  def getRealm(host: String) = "Jive SBS"

  def authorise(request: Request, method: Method, auth: Auth, resource: Resource) = {
    logger.debug("authorise")
    auth != null && auth.getTag != null
  }

  def authenticate(user: String, password: String) = {
    var authenticated = true
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user, password))
    }
    catch {
      case a: AuthenticationException => authenticated = false

    }
    authenticated.asInstanceOf[AnyRef]
  }

  def authenticate(digestRequest: DigestResponse) = {

    //val dg = new DigestGenerator();
    //val actualPassword = nameAndPasswords.get(digestRequest.getUser());
    //val serverResponse = dg.generateDigest(digestRequest, actualPassword);
    //val clientResponse = digestRequest.getResponseDigest();


    //logger.debug("server resp: " + serverResponse);
    //logger.debug("given response: " + clientResponse);

    false.asInstanceOf[AnyRef]
  }

}