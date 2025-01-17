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

import alluxio.erasurecode.rawcoder.util.CoderUtil;

import java.nio.ByteBuffer;

/**
 * An abstract raw erasure decoder that's to be inherited by new decoders.
 *
 * It implements the {@link RawErasureDecoder} interface.
 */
public abstract class AbstractRawErasureDecoder extends AbstractRawErasureCoder
    implements RawErasureDecoder {

  public AbstractRawErasureDecoder(int numDataUnits, int numParityUnits) {
    super(numDataUnits, numParityUnits);
  }

  @Override
  public void decode(ByteBuffer[] inputs, int[] erasedIndexes, ByteBuffer[] outputs) {
    checkParameters(inputs, erasedIndexes, outputs);

    ByteBuffer validInput = CoderUtil.findFirstValidInput(inputs);
    boolean usingDirectBuffer = validInput.isDirect();
    int dataLen = validInput.remaining();
    if (dataLen == 0) {
      return;
    }
    checkParameterBuffers(inputs, true, dataLen, usingDirectBuffer, false);
    checkParameterBuffers(outputs, false, dataLen, usingDirectBuffer, true);

    int[] inputPositions = new int[inputs.length];
    for (int i = 0; i < inputPositions.length; i ++) {
      if (inputs[i] != null) {
        inputPositions[i] = inputs[i].position();
      }
    }

    if (usingDirectBuffer) {
      doDecode(inputs, erasedIndexes, outputs);
    } else {
      int[] inputOffsets = new int[inputs.length];
      int[] outputOffsets = new int[outputs.length];
      byte[][] newInputs = new byte[inputs.length][];
      byte[][] newOutputs = new byte[outputs.length][];

      ByteBuffer buffer;
      for (int i = 0; i < inputs.length; ++i) {
        buffer = inputs[i];
        if (buffer != null) {
          inputOffsets[i] = buffer.arrayOffset() + buffer.position();
          newInputs[i] = buffer.array();
        }
      }

      for (int i = 0; i < outputs.length; ++i) {
        buffer = outputs[i];
        outputOffsets[i] = buffer.arrayOffset() + buffer.position();
        newOutputs[i] = buffer.array();
      }

      doDecode(newInputs, inputOffsets, dataLen, erasedIndexes, newOutputs, outputOffsets);
    }

    for (int i = 0; i < inputs.length; i ++) {
      if (inputs[i] != null) {
        // dataLen bytes consumed
        inputs[i].position(inputPositions[i] + dataLen);
      }
    }
  }

  /**
   * Perform the real decoding using Direct ByteBuffer.
   * 
   * @param inputs Direct ByteBuffers expected
   * @param erasedIndexes indexes of erased units in the inputs array
   * @param outputs Direct ByteBuffers expected
   */
  protected abstract void doDecode(ByteBuffer[] inputs, int[] erasedIndexes, ByteBuffer[] outputs);

  @Override
  public void decode(byte[][] inputs, int[] erasedIndexes, byte[][] outputs) {
    checkParameters(inputs, erasedIndexes, outputs);

    byte[] validInput = CoderUtil.findFirstValidInput(inputs);
    int dataLen = validInput.length;
    if (dataLen == 0) {
      return;
    }
    checkParameterBuffers(inputs, true, dataLen, false);
    checkParameterBuffers(outputs, false, dataLen, true);

    int[] inputOffsets = new int[inputs.length]; // ALL ZERO
    int[] outputOffsets = new int[outputs.length]; // ALL ZERO

    doDecode(inputs, inputOffsets, dataLen, erasedIndexes, outputs, outputOffsets);
  }

  /**
   * Perform the real decoding using bytes array, supporting offsets and lengths.
   * 
   * @param inputs the input byte arrays to read data from
   * @param inputOffsets offsets for the input byte arrays to read data from
   * @param dataLen how much data are to be read from
   * @param erasedIndexes indexes of erased units in the inputs array
   * @param outputs the output byte arrays to write resultant data into
   * @param outputOffsets offsets from which to write resultant data into
   */
  protected abstract void doDecode(byte[][] inputs, int[] inputOffsets, int dataLen,
      int[] erasedIndexes, byte[][] outputs, int[] outputOffsets);

  /**
   * Check and validate decoding parameters, throw exception accordingly. The checking assumes it's
   * a MDS code. Other code can override this.
   * 
   * @param inputs input buffers to check
   * @param erasedIndexes indexes of erased units in the inputs array
   * @param outputs output buffers to check
   */
  protected <T> void checkParameters(T[] inputs, int[] erasedIndexes, T[] outputs) {
    if (inputs.length != getNumParityUnits() + getNumDataUnits()) {
      throw new IllegalArgumentException("Invalid inputs length");
    }

    if (erasedIndexes.length != outputs.length) {
      throw new IllegalArgumentException("erasedIndexes and outputs mismatch in length");
    }

    if (erasedIndexes.length > getNumParityUnits()) {
      throw new IllegalArgumentException("Too many erased, not recoverable");
    }

    int validInputs = 0;
    for (T input : inputs) {
      if (input != null) {
        validInputs += 1;
      }
    }

    if (validInputs < getNumDataUnits()) {
      throw new IllegalArgumentException("No enough valid inputs are provided, not recoverable");
    }
  }
}
