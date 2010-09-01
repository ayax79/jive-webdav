package com.syncapse.jive.webdav

import com.jivesoftware.community.{JiveContainer, JiveConstants, JiveObject}
import net.sf.webdav.StoredObject

object JiveWebdavUtils {
  def buildStoredObject(jo: JiveObject) = jo.getObjectType match {
    case JiveConstants.COMMUNITY => buildStoredObjectFromContainer(jo.asInstanceOf[JiveContainer]);
    case JiveConstants.SOCIAL_GROUP => buildStoredObjectFromContainer(jo.asInstanceOf[JiveContainer]);
  }

  protected def buildStoredObjectFromContainer(jc: JiveContainer): StoredObject = {
    val so = new StoredObject
    so.setCreationDate(jc.getCreationDate)
    so.setFolder(true)
    so.setLastModified(jc.getModificationDate)
    so.setResourceLength(0L)
    so
  }

}