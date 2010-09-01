package com.syncapse.jive.webdav

import net.sf.webdav.StoredObject
import com.jivesoftware.community._

trait JiveCaseClass
case class CommunityCase(jiveObject: Community) extends JiveCaseClass
case class SocialGroupCase(jiveObject: SocialGroupCase) extends JiveCaseClass
case class DocumentCase(jiveObject: Document) extends JiveCaseClass

object JiveWebdavUtils {
  def buildStoredObject(jo: JiveCaseClass) = jo match {
    case CommunityCase(c) => buildStoredObjectFromContainer(c.asInstanceOf[JiveContainer]);
    case SocialGroupCase(c) => buildStoredObjectFromContainer(c.asInstanceOf[JiveContainer]);
  }

  protected def buildStoredObjectFromContainer(jc: JiveContainer): StoredObject = {
    val so = new StoredObject
    so.setCreationDate(jc.getCreationDate)
    so.setFolder(true)
    so.setLastModified(jc.getModificationDate)
    so.setResourceLength(0L)
    so
  }

  protected def buildContentObject(d: Document): StoredObject = {
    val so = new StoredObject
    so.setCreationDate(d.getCreationDate)
    so.setLastModified(d.getModificationDate)
    val binaryBody = d.getBinaryBody
    val size = if (binaryBody != null) {
      binaryBody.getSize
    }
    else {
      d.getPlainBody.getBytes.length.toLong
    }
    so.setResourceLength(size)
    so
  }

}