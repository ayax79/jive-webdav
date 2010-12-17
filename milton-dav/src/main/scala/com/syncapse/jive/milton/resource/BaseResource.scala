package com.syncapse.jive.milton.resource

import java.lang.String
import com.bradmcevoy.http.Request.Method
import com.bradmcevoy.http._

abstract class BaseResource(sm: SecurityManager) extends Resource with GetableResource {

  def checkRedirect(request: Request) = null

  def getRealm = "Jive SBS"

  def authorise(request: Request, method: Method, auth: Auth) = sm.authorise(request, method, auth, this)

  def authenticate(user: String, password: String) = sm.authenticate(user, password)

  def getMaxAgeSeconds(auth: Auth) = 3600L

  def getContentLength: java.lang.Long = null.asInstanceOf[Long]
}