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

import alluxio.erasurecode.rawcoder.util.ErasureCodeNative;

import java.nio.ByteBuffer;

/**
 * A Reed-Solomon raw encoder using Intel ISA-L library.
 */

public class NativeRSRawEncoder extends AbstractNativeRawEncoder {

  static {
    ErasureCodeNative.checkNativeCodeLoaded();
  }

  public NativeRSRawEncoder(int numDataUnits, int numParityUnits) {
    super(numDataUnits, numParityUnits);
    initImpl(numDataUnits, numParityUnits);
  }

  @Override
  protected void performEncodeImpl(ByteBuffer[] inputs, int[] inputOffsets, int dataLen,
      ByteBuffer[] outputs, int[] outputOffsets) {
    encodeImpl(inputs, inputOffsets, dataLen, outputs, outputOffsets);
  }

  @Override
  public void release() {
    destroyImpl();
  }

  private native void initImpl(int numDataUnits, int numParityUnits);

  private native void encodeImpl(ByteBuffer[] inputs, int[] inputOffsets, int dataLen,
      ByteBuffer[] outputs, int[] outputOffsets);

  private native void destroyImpl();
}
