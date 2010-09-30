package com.syncapse.jive.webdav

import org.acegisecurity.context.SecurityContextHolder
import org.acegisecurity.Authentication
import com.jivesoftware.base.aaa.JiveAuthentication
import com.jivesoftware.base.User

trait JiveAuthenticationProvidable {
  protected def jiveAuthentication: Option[JiveAuthentication] = {
    SecurityContextHolder.getContext.getAuthentication.asInstanceOf[JiveAuthentication] match {
      case null => None
      case a: Authentication => Some(a)
    }
  }

  protected def currentUser: Option[User] = {
    jiveAuthentication match {
      case Some(a) => Some(a.getUser)
      case None => None
    }
  }
}