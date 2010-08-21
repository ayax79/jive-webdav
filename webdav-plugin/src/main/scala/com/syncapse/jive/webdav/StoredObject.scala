package com.syncapse.jive.webdav

import org.joda.time.DateTime;

case class StoredObject(isFolder: Boolean, lastModified: DateTime, creationDate: DateTime, contentLength: Long, isNullResource: Boolean) {

  /**
   * Determines whether the StoredObject is a folder or a resource
   *
   * @return true if the StoredObject is a resource
   */
  val isResource: Boolean = !isFolder

}
