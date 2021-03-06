/**
 * Copyright 2011 wallet.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wallet.platform.listener;

/**
 * <p>Listener to provide the following to applications:</p>
 * <ul>
 * <li>Notification of a Quit event</li>
 * </ul>
 *
 * @since 0.3.0
 *         
 */
public interface GenericQuitEventListener {
    /**
     * Received when the user requests to Quit
     *
     * @param event The event
     * @param response The response to prevent the quit operation if necessary
     */
    void onQuitEvent(GenericQuitEvent event, GenericQuitResponse response);
}
