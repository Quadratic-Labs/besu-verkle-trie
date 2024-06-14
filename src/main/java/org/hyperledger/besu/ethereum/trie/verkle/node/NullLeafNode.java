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
package org.hyperledger.besu.ethereum.trie.verkle.node;

import org.hyperledger.besu.ethereum.trie.verkle.visitor.NodeVisitor;
import org.hyperledger.besu.ethereum.trie.verkle.visitor.PathNodeVisitor;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * A special node representing a null or empty node in the Verkle Trie.
 *
 * <p>
 * The `NullNode` class serves as a placeholder for non-existent nodes in the
 * Verkle Trie
 * structure. It implements the Node interface and represents a node that
 * contains no information or
 * value.
 */
public class NullLeafNode<V> extends Node<V> {

  public NullLeafNode() {
    super(false, true);
  }

  public NullLeafNode(Optional<V> previousValue) {
    super(false, true);
    this.previous = previousValue;
  }

  /**
   * Accepts a visitor for path-based operations on the node.
   *
   * @param visitor The path node visitor.
   * @param path    The path associated with a node.
   * @return The result of the visitor's operation.
   */
  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    return visitor.visit(this, path);
  }

  /**
   * Accepts a visitor for generic node operations.
   *
   * @param visitor The node visitor.
   * @return The result of the visitor's operation.
   */
  @Override
  public Node<V> accept(final NodeVisitor<V> visitor) {
    return visitor.visit(this);
  }

  /**
   * Get the hash associated with the `NullNode`.
   *
   * @return An optional containing the empty hash.
   */
  @Override
  public Optional<Bytes32> getHash() {
    return Optional.of(EMPTY_HASH);
  }

  /**
   * Get the hash associated with the `NullNode`.
   *
   * @return An optional containing the empty hash.
   */
  @Override
  public Optional<Bytes> getCommitment() {
    return Optional.of(EMPTY_COMMITMENT);
  }

  @Override
  public void markDirty() {
    dirty = true;
  }

  @Override
  public Boolean isNull() {
    return true;
  }

  /**
   * Get a string representation of the `NullNode`.
   *
   * @return A string representation indicating that it is a "NULL" node.
   */
  @Override
  public String print() {
    return "[NULL-LEAF]";
  }

  /**
   * Generates DOT representation for the NullLeafNode.
   *
   * @return DOT representation of the NullLeafNode.
   */
  @Override
  public String toDot(Boolean showRepeatingEdges) {
    if (!showRepeatingEdges) {
      return "";
    }
    String result = getClass().getSimpleName()
        + getLocation().orElse(Bytes.EMPTY)
        + " [label=\"NL: "
        + getLocation().orElse(Bytes.EMPTY)
        + "\"]\n";
    return result;
  }
}
