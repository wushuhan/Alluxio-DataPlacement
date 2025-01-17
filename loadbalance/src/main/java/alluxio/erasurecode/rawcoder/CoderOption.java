/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package alluxio.erasurecode.rawcoder;

/**
 * Supported erasure coder options.
 */
public enum CoderOption {
  /* If direct buffer is preferred, for perf consideration */
  PREFER_DIRECT_BUFFER(true), // READ-ONLY
  /**
   * Allow changing input buffer content (not positions). Maybe better perf if allowed
   */
  ALLOW_CHANGE_INPUTS(false), // READ-WRITE
  /* Allow dump verbose debug info or not */
  ALLOW_VERBOSE_DUMP(false); // READ-WRITE

  private boolean isReadOnly = false;

  CoderOption(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
  }

  public boolean isReadOnly() {
    return isReadOnly;
  }
};
