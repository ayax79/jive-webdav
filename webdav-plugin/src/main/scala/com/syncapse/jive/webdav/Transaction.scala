package com.syncapse.jive.webdav

import java.security.Principal;

trait Transaction {
  def getPrincipal: Principal;
}
