package com.syncapse.jive.webdav

trait MimeTyper {
  def getMimeType(path: String): String
}
