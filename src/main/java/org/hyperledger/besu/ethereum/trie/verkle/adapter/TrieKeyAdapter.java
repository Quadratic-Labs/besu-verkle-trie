/*
 * Copyright Hyperledger Besu Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.hyperledger.besu.ethereum.trie.verkle.adapter;

import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.BASIC_DATA_LEAF_KEY;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.CODE_HASH_LEAF_KEY;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.CODE_OFFSET;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.HEADER_STORAGE_OFFSET;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.HEADER_STORAGE_SIZE;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.MAIN_STORAGE_OFFSET_SHIFT_LEFT_VERKLE_NODE_WIDTH;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.VERKLE_NODE_WIDTH;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.VERKLE_NODE_WIDTH_LOG2;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.PedersenHasher;

import java.util.ArrayList;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/**
 * Utility class for generating keys used in a Verkle Trie.
 *
 * <p>The `TrieKeyAdapter` class provides methods for generating various keys, such as storage keys,
 * code chunk keys, and header keys, used in a Verkle Trie structure.
 */
public class TrieKeyAdapter {

  private static final int CHUNK_SIZE = 31;

  private final Hasher hasher;

  /** Creates a TrieKeyAdapter with the default Perdersen hasher. */
  public TrieKeyAdapter() {
    this.hasher = new PedersenHasher();
  }

  /**
   * Creates a TrieKeyAdapter with the provided hasher.
   *
   * @param hasher The hasher used for key generation.
   */
  public TrieKeyAdapter(final Hasher hasher) {
    this.hasher = hasher;
  }

  /**
   * Retrieves the hasher used for key generation
   *
   * @return The {@link Hasher} instance used for key generation operations.
   */
  public Hasher getHasher() {
    return hasher;
  }

  /**
   * Generates a storage key for a given address and storage key.
   *
   * @param address The address.
   * @param storageKey The storage key.
   * @return The generated storage key.
   */
  public Bytes32 storageKey(final Bytes address, final Bytes32 storageKey) {
    final Bytes stem = getStorageStem(address, storageKey);
    final Bytes suffix = getStorageKeySuffix(storageKey);
    return Bytes32.wrap(Bytes.concatenate(stem, suffix));
  }

  public UInt256 getStorageKeyTrieIndex(final Bytes32 storageKey) {
    final UInt256 uintStorageKey = UInt256.fromBytes(storageKey);
    if (uintStorageKey.compareTo(HEADER_STORAGE_SIZE) < 0) {
      return uintStorageKey.add(HEADER_STORAGE_OFFSET).divide(VERKLE_NODE_WIDTH);
    } else {
      // We divide by VerkleNodeWidthLog2 to make space and prevent any potential overflow
      // Then, we increment, a step that is safeguarded against overflow.
      return uintStorageKey
          .shiftRight(VERKLE_NODE_WIDTH_LOG2.intValue())
          .add(MAIN_STORAGE_OFFSET_SHIFT_LEFT_VERKLE_NODE_WIDTH);
    }
  }

  public Bytes getStorageKeySuffix(final Bytes32 storageKey) {
    final UInt256 uintStorageKey = UInt256.fromBytes(storageKey);
    if (uintStorageKey.compareTo(HEADER_STORAGE_SIZE) < 0) {
      return getLastByte(uintStorageKey.add(HEADER_STORAGE_OFFSET).mod(VERKLE_NODE_WIDTH));
    } else {
      return getLastByte(storageKey);
    }
  }

  public Bytes getStorageStem(final Bytes address, final Bytes32 storageKey) {
    final UInt256 trieIndex = getStorageKeyTrieIndex(storageKey);
    return hasher.computeStem(address, trieIndex);
  }

  /**
   * Generates a code chunk key for a given address and chunkId.
   *
   * @param address The address.
   * @param chunkId The chunk ID.
   * @return The generated code chunk key.
   */
  public Bytes32 codeChunkKey(final Bytes address, final UInt256 chunkId) {
    final Bytes stem = getCodeChunkStem(address, chunkId);
    final Bytes suffix = getCodeChunkKeySuffix(chunkId);
    return Bytes32.wrap(Bytes.concatenate(stem, suffix));
  }

  public UInt256 getCodeChunkKeyTrieIndex(final Bytes32 chunkId) {
    return CODE_OFFSET.add(UInt256.fromBytes(chunkId)).divide(VERKLE_NODE_WIDTH);
  }

  public Bytes getCodeChunkKeySuffix(final Bytes32 chunkId) {
    return getLastByte(CODE_OFFSET.add(UInt256.fromBytes(chunkId)).mod(VERKLE_NODE_WIDTH));
  }

  public Bytes getCodeChunkStem(final Bytes address, final UInt256 chunkId) {
    final UInt256 trieIndex = getCodeChunkKeyTrieIndex(chunkId);
    return hasher.computeStem(address, trieIndex);
  }

  /**
   * Generates a header key for a given address and leafKey.
   *
   * @param address The address.
   * @param leafKey The leaf key.
   * @return The generated header key.
   */
  public Bytes32 headerKey(final Bytes address, final UInt256 leafKey) {
    final Bytes stem = getHeaderStem(address);
    return Bytes32.wrap(Bytes.concatenate(stem, getLastByte(leafKey)));
  }

  public Bytes getHeaderStem(final Bytes address) {
    return hasher.computeStem(address, UInt256.valueOf(0).toBytes());
  }

  /**
   * Get the last byte of the base .
   *
   * @param base The base key.
   * @return The last byte of the key.
   */
  public Bytes getLastByte(final Bytes32 base) {
    return base.slice(Bytes32.SIZE - 1);
  }

  /**
   * Generates a basic data key for a given address.
   *
   * @param address The address.
   * @return The generated version key.
   */
  public Bytes32 basicDataKey(Bytes address) {
    return headerKey(address, BASIC_DATA_LEAF_KEY);
  }

  /**
   * Generates a code hash key for a given address.
   *
   * @param address The address.
   * @return The generated code Keccak key.
   */
  public Bytes32 codeHashKey(Bytes address) {
    return headerKey(address, CODE_HASH_LEAF_KEY);
  }

  public int getNbChunk(final Bytes bytecode) {
    return bytecode.isEmpty() ? 0 : (1 + ((bytecode.size() - 1) / CHUNK_SIZE));
  }

  /**
   * Chunk code's bytecode for insertion in the Trie. Each chunk code uses its position in the list
   * as chunkId
   *
   * @param bytecode Code's bytecode
   * @return List of 32-bytes code chunks
   */
  public List<UInt256> chunkifyCode(final Bytes bytecode) {
    if (bytecode.isEmpty()) {
      return new ArrayList<>();
    }

    // Chunking variables
    final int CHUNK_SIZE = 31;
    final int nChunks = getNbChunk(bytecode);
    final int padSize = nChunks * CHUNK_SIZE - bytecode.size();
    final Bytes code = Bytes.concatenate(bytecode, Bytes.repeat((byte) 0, padSize));
    final List<UInt256> chunks = new ArrayList<>(nChunks);

    // OpCodes for PUSH's
    final int PUSH_OFFSET = 95;
    final int PUSH1 = PUSH_OFFSET + 1;
    final int PUSH32 = PUSH_OFFSET + 32;

    // Iterator data
    int chunkPos = 0; // cursor position to start of current chunk
    int posInChunk = 0; // cursor position relative to the current chunk
    int nPushData = 0; // number of bytes in current push data

    // Create chunk iteratively
    for (int chunkId = 0; chunkId < nChunks; ++chunkId) {
      chunkPos = chunkId * CHUNK_SIZE;
      posInChunk = nPushData;
      while (posInChunk < CHUNK_SIZE) {
        int opCode = Byte.toUnsignedInt(code.get(chunkPos + posInChunk));
        posInChunk += 1;
        if (PUSH1 <= opCode && opCode <= PUSH32) {
          posInChunk += opCode - PUSH_OFFSET;
        }
      }
      chunks.add(
          UInt256.fromBytes(
              Bytes.concatenate(
                  Bytes.of(Math.min(nPushData, 31)), code.slice(chunkPos, CHUNK_SIZE))));
      nPushData = posInChunk - CHUNK_SIZE;
    }

    return chunks;
  }
}
