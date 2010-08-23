package com.syncapse.jive.webdav

import java.security.Principal
import java.io.InputStream
import java.lang.String
import net.sf.webdav.{ITransaction, IWebdavStore}
import com.jivesoftware.community.{DocumentManager, CommunityManager}

class JiveWebdavStore(communityMgr: CommunityManager, documentManager: DocumentManager) extends IWebdavStore {


  def getStoredObject(transaction: ITransaction, uri: String) = {



    
  }

  def removeObject(transaction: ITransaction, uri: String) = {



  }

  def getResourceLength(transaction: ITransaction, path: String) = 0L

  def getChildrenNames(transaction: ITransaction, folderUri: String) = {
    communityMgr.getCommunity




  }

  def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = 0L

  def getResourceContent(transaction: ITransaction, resourceUri: String) = null

  def createResource(transaction: ITransaction, resourceUri: String) = {}

  def createFolder(transaction: ITransaction, folderUri: String) = {}

  def rollback(transaction: ITransaction) = {}

  def commit(transaction: ITransaction) = {}

  def checkAuthentication(transaction: ITransaction) = {}

  def begin(principal: Principal) = null
}