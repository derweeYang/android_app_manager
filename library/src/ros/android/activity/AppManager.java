/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ros.android.activity;

import org.ros.Subscriber;

import java.util.ArrayList;

import org.ros.exception.RosInitException;

import org.ros.MessageListener;
import org.ros.message.app_manager.AppList;

import org.ros.Node;
import org.ros.ServiceResponseListener;
import org.ros.internal.node.service.ServiceClient;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.namespace.NameResolver;
import org.ros.service.app_manager.ListApps;
import org.ros.service.app_manager.StartApp;
import org.ros.service.app_manager.StopApp;

/**
 * Interact with a remote ROS App Manager.
 * 
 * @author kwc@willowgarage.com (Ken Conley)
 */
public class AppManager {
  static public final String PACKAGE = "ros.android.activity";

  private final Node node;
  private AppManagerIdentifier appManagerIdentifier;
  private AppList appList;
  private ArrayList<Subscriber<AppList>> subscriptions;
  private NameResolver resolver;

  public AppManager(AppManagerIdentifier appManagerIdentifier, Node node, NameResolver resolver)
      throws RosInitException {
    this.node = node;
    this.appManagerIdentifier = appManagerIdentifier;
    this.resolver = resolver;
    subscriptions = new ArrayList<Subscriber<AppList>>();
    addAppListCallback(new MessageListener<AppList>() {

      @Override
      public void onNewMessage(AppList message) {
        appList = message;
      }
    });

  }

  public void addAppListCallback(MessageListener<AppList> callback) throws RosInitException {
    subscriptions.add(node.createSubscriber(resolver.resolveName("app_list"), callback,
        AppList.class));
  }

  public AppList getAppList() {
    return appList;
  }

  public void listApps(final ServiceResponseListener<ListApps.Response> callback) {
    ServiceIdentifier serviceIdentifier = appManagerIdentifier.getListAppsIdentifier();
    try {
      ServiceClient<ListApps.Response> listAppsClient = node.createServiceClient(serviceIdentifier,
          ListApps.Response.class);
      listAppsClient.call(new ListApps.Request(), callback);
    } catch (Throwable ex) {
      callback.onFailure(new Exception(ex));
    }
  }

  public void startApp(final String appName,
      final ServiceResponseListener<StartApp.Response> callback) {
    ServiceIdentifier serviceIdentifier = appManagerIdentifier.getStartAppIdentifier();
    try {
      ServiceClient<StartApp.Response> startAppClient = node.createServiceClient(serviceIdentifier,
          StartApp.Response.class);
      StartApp.Request request = new StartApp.Request();
      request.name = appName;
      startAppClient.call(request, callback);
    } catch (Throwable ex) {
      callback.onFailure(new Exception(ex));
    }
  }

  public void stopApp(final String appName, final ServiceResponseListener<StopApp.Response> callback) {
    ServiceIdentifier serviceIdentifier = appManagerIdentifier.getStopAppIdentifier();
    try {
      ServiceClient<StopApp.Response> stopAppClient = node.createServiceClient(serviceIdentifier,
          StopApp.Response.class);
      StopApp.Request request = new StopApp.Request();
      request.name = appName;
      stopAppClient.call(request, callback);
    } catch (Throwable ex) {
      callback.onFailure(new Exception(ex));
    }
  }

  /**
   * Blocks until App Manager is located.
   * 
   * @param node
   * @param robotName
   * @return
   * @throws AppManagerNotAvailableException
   * @throws RosInitException
   */
  public static AppManager create(Node node, String robotName) throws XmlRpcTimeoutException,
      AppManagerNotAvailableException, RosInitException {
    NameResolver resolver = node.getResolver().createResolver(robotName);
    try {
      ServiceIdentifier serviceIdentifier = node.lookupService(resolver.resolveName("list_apps"),
                                                               new ListApps());
      if (serviceIdentifier == null) {
        throw new AppManagerNotAvailableException();
      }
      AppManagerIdentifier appManagerIdentifier = new AppManagerIdentifier(resolver,
                                                                           serviceIdentifier.getUri());
      return new AppManager(appManagerIdentifier, node, resolver);
    } catch( java.lang.RuntimeException ex ) {
      throw new AppManagerNotAvailableException( ex );
    }
  }
}
