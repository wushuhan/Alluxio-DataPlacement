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

import java.nio.ByteBuffer;

/**
 * Abstract native raw encoder for all native coders to extend with.
 */

abstract class AbstractNativeRawEncoder extends AbstractRawErasureEncoder {

  public AbstractNativeRawEncoder(int numDataUnits, int numParityUnits) {
    super(numDataUnits, numParityUnits);
  }

  @Override
  protected void doEncode(ByteBuffer[] inputs, ByteBuffer[] outputs) {
    int[] inputOffsets = new int[inputs.length];
    int[] outputOffsets = new int[outputs.length];
    int dataLen = inputs[0].remaining();

    ByteBuffer buffer;
    for (int i = 0; i < inputs.length; ++i) {
      buffer = inputs[i];
      inputOffsets[i] = buffer.position();
    }

    for (int i = 0; i < outputs.length; ++i) {
      buffer = outputs[i];
      outputOffsets[i] = buffer.position();
    }

    performEncodeImpl(inputs, inputOffsets, dataLen, outputs, outputOffsets);

    for (int i = 0; i < inputs.length; ++i) {
      buffer = inputs[i];
      buffer.position(inputOffsets[i] + dataLen); // dataLen bytes consumed
    }
  }

  protected abstract void performEncodeImpl(ByteBuffer[] inputs, int[] inputOffsets, int dataLen,
      ByteBuffer[] outputs, int[] outputOffsets);

  @Override
  protected void doEncode(byte[][] inputs, int[] inputOffsets, int dataLen, byte[][] outputs,
      int[] outputOffsets) {
    System.out
        .println("WARNING: doEncodeByConvertingToDirectBuffers " + "is used, not efficiently");
    doEncodeByConvertingToDirectBuffers(inputs, inputOffsets, dataLen, outputs, outputOffsets);
  }

  /**
   * Perform the encoding work using bytes array, via converting to ByteBuffers. Please note this
   * may be not efficient and serves as fall-back. We should avoid calling into this.
   * 
   * @param inputs
   * @param outputs
   */
  private void doEncodeByConvertingToDirectBuffers(byte[][] inputs, int[] inputOffsets, int dataLen,
      byte[][] outputs, int[] outputOffsets) {
    ByteBuffer[] inputBuffers = new ByteBuffer[inputs.length];
    ByteBuffer[] outputBuffers = new ByteBuffer[outputs.length];

    for (int i = 0; i < inputs.length; ++i) {
      inputBuffers[i] = convertInputBuffer(inputs[i], inputOffsets[i], dataLen);
    }

    for (int i = 0; i < outputs.length; ++i) {
      outputBuffers[i] = convertOutputBuffer(outputs[i], dataLen);
    }

    doEncode(inputBuffers, outputBuffers);

    for (int i = 0; i < outputs.length; i ++) {
      outputBuffers[i].get(outputs[i], outputOffsets[i], dataLen);
    }
  }

  /**
   * @return false as output buffers can be memset-ed efficiently in native codes.
   */
  @Override
  protected boolean wantInitOutputs() {
    return false;
  }

  /**
   * @return true as native codes like direct or off-heap buffers.
   */
  @Override
  protected boolean preferDirectBuffer() {
    return true;
  }

  // To link with the underlying data structure in the native layer.
  private long __native_coder;
  // Whether to allow verbose dump in the native layer for debugging.
  private long __native_verbose = 0;
}
