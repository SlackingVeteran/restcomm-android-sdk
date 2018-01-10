/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * For questions related to commercial use licensing, please contact sales@telestax.com.
 *
 */

package org.restcomm.android.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import org.restcomm.android.sdk.RCClient;
import org.restcomm.android.sdk.RCConnection;
import org.restcomm.android.sdk.RCDevice;
import org.restcomm.android.sdk.fcm.FcmConfigurationHandler;
import org.restcomm.android.sdk.storage.StorageManagerInterface;
import org.restcomm.android.sdk.storage.StorageManagerPreferences;
import org.restcomm.android.sdk.storage.StorageUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Various internal SDK utilities not to be directly used by App
 */
public class RCUtils {
   private static final String TAG = "RCUtils";

   public static void validateDeviceParms(HashMap<String, Object> parameters, Map<String, ?> storageManagerInterfaceParams) throws RCException
   {
      validateSettingsParms(parameters, storageManagerInterfaceParams);

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.INTENT_INCOMING_CALL)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.INTENT_INCOMING_CALL)) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_CALL_INTENT);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.INTENT_INCOMING_MESSAGE)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.INTENT_INCOMING_MESSAGE)) {
            RCLogger.w(TAG, "validateDeviceParms(): Intent missing for incoming text messages, your App will work but won't be able to be notified on such event");
         }
      }
      //return new ErrorStruct(RCClient.ErrorCodes.SUCCESS);
   }

   public static void validateSettingsParms(HashMap<String, Object> parameters, Map<String, ?> storageManagerInterfaceParams) throws RCException
   {
      validatePushSettings(parameters, storageManagerInterfaceParams);
      /*
      if (parameters.containsKey(RCDevice.ParameterKeys.MEDIA_TURN_ENABLED) &&
            ((Boolean)parameters.get(RCDevice.ParameterKeys.MEDIA_TURN_ENABLED))) {
       */

      //we will raise an exception only if we don't have param saved on storage manager;
      // If we have it and user did not send it we are good.

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.SIGNALING_USERNAME)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.SIGNALING_USERNAME) ||
                 parameters.get(RCDevice.ParameterKeys.SIGNALING_USERNAME).equals("")) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_USERNAME);
            //return new ErrorStruct(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_USERNAME);
         }
      }

      if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS_DISCOVERY_TYPE)) {
         // discovery type not provided
         parameters.put(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS_DISCOVERY_TYPE, RCDevice.MediaIceServersDiscoveryType.ICE_SERVERS_CONFIGURATION_URL_XIRSYS_V2);
      } else {
         // discovery type provided
         RCDevice.MediaIceServersDiscoveryType iceServersDiscoveryType;
         if (parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS_DISCOVERY_TYPE) instanceof Enum){
            iceServersDiscoveryType = (RCDevice.MediaIceServersDiscoveryType)parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS_DISCOVERY_TYPE);
         } else {
            iceServersDiscoveryType = RCDevice.MediaIceServersDiscoveryType.values()[(int)parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS_DISCOVERY_TYPE)];
         }

         if (iceServersDiscoveryType.ordinal() < RCDevice.MediaIceServersDiscoveryType.ICE_SERVERS_CONFIGURATION_URL_XIRSYS_V2.ordinal() ||
                 iceServersDiscoveryType.ordinal() > RCDevice.MediaIceServersDiscoveryType.ICE_SERVERS_CUSTOM.ordinal()) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_ICE_SERVER_DISCOVERY_TYPE);
         }

         if (iceServersDiscoveryType == RCDevice.MediaIceServersDiscoveryType.ICE_SERVERS_CUSTOM) {
            // custom (i.e. no configuration url used)

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS)) {
               if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS)) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY_NO_ICE_SERVERS);
               } else {
                  List<Map<String, String>> iceServers = (List<Map<String, String>>) parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS);
                  if (iceServers.size() == 0) {
                     throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY_NO_ICE_SERVERS);
                  }
                  for (Map<String, String> iceServer : iceServers) {
                     if (!iceServer.containsKey(RCConnection.IceServersKeys.ICE_SERVER_URL) ||
                             iceServer.get(RCConnection.IceServersKeys.ICE_SERVER_URL).equals("")) {
                        throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY);
                     }
                  }
               }
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_URL) && parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_URL)) {
               throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY);
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_USERNAME) && parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_USERNAME)) {
               throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY);
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_PASSWORD) && parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_PASSWORD)) {
               throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY);
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_DOMAIN) && parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_DOMAIN)) {
               throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_CUSTOM_DISCOVERY);
            }

         } else {
            // not custom; media ice servers shouldn't be provided
            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS)) {
               if (parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_SERVERS)) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_INVALID_ICE_SERVERS_NOT_CUSTOM_DISCOVERY);
               }
            }

            // all those fields are mandatory when configuration URL is used
            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_URL)) {
               if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_URL) ||
                       parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_URL).equals("")) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_ICE_URL);
               }
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_USERNAME)) {
               if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_USERNAME) ||
                       parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_USERNAME).equals("")) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_ICE_USERNAME);
               }
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_PASSWORD)) {
               if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_PASSWORD) ||
                       parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_PASSWORD).equals("")) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_ICE_PASSWORD);
               }
            }

            if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_DOMAIN)) {
               if (!parameters.containsKey(RCDevice.ParameterKeys.MEDIA_ICE_DOMAIN) ||
                       parameters.get(RCDevice.ParameterKeys.MEDIA_ICE_DOMAIN).equals("")) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_MISSING_ICE_DOMAIN);
               }
            }
         }
      }
   }

   public static void validateConnectionParms(HashMap<String, Object> parameters, Map<String, ?>  storageManagerInterfaceParams) throws RCException
   {
      if (!storageManagerInterfaceParams.containsKey(RCConnection.ParameterKeys.CONNECTION_PEER)) {
         if (!parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_PEER) ||
                 parameters.get(RCConnection.ParameterKeys.CONNECTION_PEER).equals("")) {
            throw new RCException(RCClient.ErrorCodes.ERROR_CONNECTION_MISSING_PEER);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCConnection.ParameterKeys.CONNECTION_VIDEO_ENABLED)) {
         if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_VIDEO_ENABLED) &&
                 (boolean) parameters.get(RCConnection.ParameterKeys.CONNECTION_VIDEO_ENABLED)) {
            // video call

            if (!storageManagerInterfaceParams.containsKey(RCConnection.ParameterKeys.CONNECTION_LOCAL_VIDEO)) {
               if (!parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_LOCAL_VIDEO) ||
                       parameters.get(RCConnection.ParameterKeys.CONNECTION_LOCAL_VIDEO) == null ||
                       !parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_REMOTE_VIDEO) ||
                       parameters.get(RCConnection.ParameterKeys.CONNECTION_REMOTE_VIDEO) == null) {
                  throw new RCException(RCClient.ErrorCodes.ERROR_CONNECTION_VIDEO_CALL_VIEWS_MANDATORY);
               }
            }
         } else {
            // audio-only call
            if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_LOCAL_VIDEO)) {
               RCLogger.w(TAG, "validateConnectionParms(): WARN, local video  doesn't take effect since the call is audio-only" + parameters.toString());
            }

            if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_REMOTE_VIDEO)) {
               RCLogger.w(TAG, "validateConnectionParms(): WARN, remote video doesn't take effect since the call is audio-only" + parameters.toString());
            }

            if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_PREFERRED_VIDEO_CODEC)) {
               // should not throw exception because we 'll be breaking backwards compatibility pretty badly. Let's instead log this
               //throw new RCException(RCClient.ErrorCodes.ERROR_CONNECTION_AUDIO_CALL_VIDEO_CODEC_FORBIDDEN);
               RCLogger.w(TAG, "validateConnectionParms(): WARN, video codec doesn't take effect since the call is audio-only" + parameters.toString());
            }
            if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_PREFERRED_VIDEO_RESOLUTION)) {
               //throw new RCException(RCClient.ErrorCodes.ERROR_CONNECTION_AUDIO_CALL_VIDEO_RESOLUTION_FORBIDDEN);
               RCLogger.w(TAG, "validateConnectionParms(): WARN, video resolution doesn't take effect since the call is audio-only" + parameters.toString());
            }
            if (parameters.containsKey(RCConnection.ParameterKeys.CONNECTION_PREFERRED_VIDEO_FRAME_RATE)) {
               //throw new RCException(RCClient.ErrorCodes.ERROR_CONNECTION_AUDIO_CALL_VIDEO_FRAME_RATE_FORBIDDEN);
               RCLogger.w(TAG, "validateConnectionParms(): WARN, video frame rate doesn't take effect since the call is audio-only" + parameters.toString());
            }

         }
      }

   }

   static boolean validatePushSettings(HashMap<String, Object> parameters, Map<String, ?>  storageManagerInterfaceParams) throws RCException{
      //check is there fcm server key, if not write a warning
      if (parameters == null) {
         return false;
      }


      //we will raise an exception only if we don't have param saved on storage manager;
      // If we have it and user did not send it we are good.

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ENABLE_PUSH_FOR_ACCOUNT) &&
              (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ENABLE_PUSH_FOR_ACCOUNT))){
         throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_ENABLE_DISABLE_PUSH_NOTIFICATION);
      }

      if (parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ENABLE_PUSH_FOR_ACCOUNT)!=null
         && !(Boolean) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ENABLE_PUSH_FOR_ACCOUNT)){
            return true;
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_FCM_SERVER_KEY_MISSING);
         }
      }

      //check empty fields
      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_APPLICATION_NAME_MISSING);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_EMAIL)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_EMAIL) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_EMAIL) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_EMAIL))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_ACCOUNT_EMAIL_MISSING);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_ACCOUNT_PASSWORD_MISSING);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_PUSH_DOMAIN_MISSING);
         }
      }

      if (!storageManagerInterfaceParams.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN)) {
         if (!parameters.containsKey(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN) ||
                 parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN) == null ||
                 TextUtils.isEmpty((String) parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN))) {
            throw new RCException(RCClient.ErrorCodes.ERROR_DEVICE_PUSH_NOTIFICATION_RESTCOMM_DOMAIN_MISSING);
         }
      }

      return false;

   }

   public static boolean shouldRegisterForPush(HashMap<String, Object> parameters, StorageManagerInterface storageManagerInterface) {

      //when binding is missing we need to register for push
      if (TextUtils.isEmpty(storageManagerInterface.getString(FcmConfigurationHandler.FCM_BINDING, null))){
         RCLogger.v(TAG, "shouldRegisterForPush: FCM_BINDING is missing, we should register");
         return true;
      }

      if (!parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY).equals
              (storageManagerInterface.getString(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_FCM_SERVER_KEY, null))) {
         RCLogger.v(TAG, "shouldRegisterForPush: FCM Server key is different than saved one, we should register");
         return true;
      }

      if (!parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME).equals
              (storageManagerInterface.getString(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_APPLICATION_NAME, null))) {
         RCLogger.v(TAG, "shouldRegisterForPush: Application name key is different than saved one, we should register");
         return true;
      }

      if (!parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD).equals
              (storageManagerInterface.getString(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_ACCOUNT_PASSWORD, null))) {
         RCLogger.v(TAG, "shouldRegisterForPush: account password key is different than saved one, we should register");
         return true;
      }

      if (!parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN).equals
              (storageManagerInterface.getString(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_PUSH_DOMAIN, null))) {
         RCLogger.v(TAG, "shouldRegisterForPush: push domain key is different than saved one, we should register");
         return true;
      }

      if (!parameters.get(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN).equals
              (storageManagerInterface.getString(RCDevice.ParameterKeys.PUSH_NOTIFICATIONS_HTTP_DOMAIN, null))) {
         RCLogger.v(TAG, "shouldRegisterForPush: http key is different than saved one, we should register");
         return true;
      }
      RCLogger.v(TAG, "shouldRegisterForPush: Nothing is change, we shouldn't register");
      return false;
   }
}
