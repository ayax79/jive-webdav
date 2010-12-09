package com.syncapse.jive.milton

import com.jivesoftware.community.web.struts.mapping.URLMapping
import org.apache.struts2.dispatcher.mapper.ActionMapping
import java.lang.String

class WebdavUrlMapper extends URLMapping {
  def process(uri: String, mapping: ActionMapping) = mapping.setName("milton")
}