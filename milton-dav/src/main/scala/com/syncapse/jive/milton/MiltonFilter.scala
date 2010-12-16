package com.syncapse.jive.milton

import javax.servlet._
import com.bradmcevoy.http.{ServletRequest => WDRequest, ServletResponse => WDResponse}
import http.{HttpServletResponse, HttpServletRequest}

class MiltonFilter extends Filter with WebdavManagerComponent {

  def doFilter(p1: ServletRequest, p2: ServletResponse, p3: FilterChain) =
    webdavManger.process(new WDRequest(p1.asInstanceOf[HttpServletRequest]), new WDResponse(p2.asInstanceOf[HttpServletResponse]))

  def init(p1: FilterConfig) = {}

  def destroy = {}

}