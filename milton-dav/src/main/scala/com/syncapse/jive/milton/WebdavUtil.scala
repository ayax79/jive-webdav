package com.syncapse.jive.milton

import com.jivesoftware.community.{DocumentManager, Community, CommunityManager}
import collection.JavaConversions._

object WebdavUtil {


  def childCommunities(c: Community, cm: CommunityManager) = asScalaIterator(cm.getCommunities(c)).toList

  def childDocuments(c: Community, dm: DocumentManager) = asScalaIterator(dm.getDocuments(c)).filter(d1 => !d1.isTextBody).toList


}