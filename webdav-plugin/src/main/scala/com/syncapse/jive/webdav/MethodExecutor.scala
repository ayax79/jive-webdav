package com.syncapse.jive.webdav

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


trait MethodExecutor {
  def execute(transaction: Nothing, req: HttpServletRequest, resp: HttpServletResponse)
}

