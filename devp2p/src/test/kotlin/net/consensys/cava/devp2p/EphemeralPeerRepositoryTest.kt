/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.devp2p

import kotlinx.coroutines.runBlocking
import net.consensys.cava.crypto.SECP256K1
import net.consensys.cava.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
internal class EphemeralPeerRepositoryTest {

  private lateinit var peerRepository: PeerRepository
  private var currentTime: Long = System.currentTimeMillis()

  @BeforeEach
  fun setup() {
    peerRepository = EphemeralPeerRepository()
  }

  @Test
  fun shouldReturnPeerWithNoEndpointForUnknownId() = runBlocking {
    val peer = peerRepository.get(SECP256K1.KeyPair.random().publicKey())
    assertNull(peer.endpoint)
    assertNull(peer.lastSeen)
    assertNull(peer.lastVerified)
  }

  @Test
  fun shouldReturnPeerBasedOnURIWithEndpoint() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:7654"
    )
    assertNotNull(peer.endpoint)
    assertNull(peer.lastSeen)
    assertNull(peer.lastVerified)

    val expectedId = SECP256K1.PublicKey.fromHexString(
      "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705" +
        "b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"
    )
    assertEquals(expectedId, peer.nodeId)
    assertEquals(InetAddress.getByName("172.20.0.4"), peer.endpoint?.address)
    assertEquals(7654, peer.endpoint?.udpPort)
    assertEquals(7654, peer.endpoint?.tcpPort)
  }

  @Test
  fun shouldReturnPeerWithDefaultPortsWhenMissingFromURI() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4"
    )
    assertNotNull(peer.endpoint)
    assertNull(peer.lastSeen)
    assertNull(peer.lastVerified)

    val expectedId = SECP256K1.PublicKey.fromHexString(
      "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705" +
        "b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"
    )
    assertEquals(expectedId, peer.nodeId)
    assertEquals(InetAddress.getByName("172.20.0.4"), peer.endpoint?.address)
    assertEquals(30303, peer.endpoint?.udpPort)
    assertEquals(30303, peer.endpoint?.tcpPort)
  }

  @Test
  fun shouldReturnPeerWithDifferentPortsWhenQueryParamInURI() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=23456"
    )
    assertNotNull(peer.endpoint)
    assertNull(peer.lastSeen)
    assertNull(peer.lastVerified)

    val expectedId = SECP256K1.PublicKey.fromHexString(
      "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54c705" +
        "b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"
    )
    assertEquals(expectedId, peer.nodeId)
    assertEquals(InetAddress.getByName("172.20.0.4"), peer.endpoint?.address)
    assertEquals(23456, peer.endpoint?.udpPort)
    assertEquals(54789, peer.endpoint?.tcpPort)
  }

  @Test
  fun shouldThrowWhenNotEnodeURI() {
    assertThrows<IllegalArgumentException> {
      runBlocking {
        peerRepository.get(
          "http://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
            "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:30303"
        )
      }
    }
  }

  @Test
  fun shouldThrowWhenNoNodeIdInURI() {
    assertThrows<IllegalArgumentException> {
      runBlocking {
        peerRepository.get("enode://172.20.0.4:30303")
      }
    }
  }

  @Test
  fun shouldThrowWhenInvalidPortInURI() {
    assertThrows<IllegalArgumentException> {
      runBlocking {
        peerRepository.get(
          "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
            "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:98766"
        )
      }
    }
  }

  @Test
  fun shouldThrowWhenOutOfRangeDiscPortInURI() {
    assertThrows<IllegalArgumentException> {
      runBlocking {
        peerRepository.get(
          "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
            "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=98765"
        )
      }
    }
  }

  @Test
  fun shouldThrowWhenInvalidDiscPortInURI() {
    assertThrows<IllegalArgumentException> {
      runBlocking {
        peerRepository.get(
          "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
            "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789?discport=abcd"
        )
      }
    }
  }

  @Test
  fun shouldIgnoreAdditionalQueryParametersInURI() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b" +
        "@172.20.0.4:54789?foo=bar&discport=23456&bar=foo"
    )
    assertNotNull(peer.endpoint)
    assertNull(peer.lastSeen)
    assertNull(peer.lastVerified)

    val expectedId = SECP256K1.PublicKey.fromHexString(
      "c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b"
    )
    assertEquals(expectedId, peer.nodeId)
    assertEquals(InetAddress.getByName("172.20.0.4"), peer.endpoint?.address)
    assertEquals(23456, peer.endpoint?.udpPort)
    assertEquals(54789, peer.endpoint?.tcpPort)
  }

  @Test
  fun shouldNotOverwriteEndpointByDefault() = runBlocking {
    val peer1 = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789"
    )
    assertNotNull(peer1.endpoint)
    assertNull(peer1.lastSeen)
    assertNull(peer1.lastVerified)
    val endpoint1 = peer1.endpoint!!

    val peer2 = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.5:9876"
    )
    assertSame(peer2, peer1)
    assertEquals(endpoint1, peer2.endpoint)

    val peer3 = peerRepository.get(
      overwriteEndpoint = true,
      uri = "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.5:9876"
    )
    assertSame(peer3, peer1)
    assertNotEquals(endpoint1, peer3.endpoint)
    assertNull(peer3.lastSeen)
    assertNull(peer3.lastVerified)
  }

  @Test
  fun shouldUpdateEndpointIfNoTimeCriteriaSpecified() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789"
    )
    peer.verifyEndpoint(peer.endpoint!!, currentTime)
    assertEquals(currentTime, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)

    val endpoint = Endpoint("127.0.0.1", 30303, 30303)
    peer.updateEndpoint(endpoint, currentTime + 10)
    assertEquals(endpoint, peer.endpoint)
    assertEquals(currentTime + 10, peer.lastSeen)
    assertNull(peer.lastVerified)
  }

  @Test
  fun shouldNotUpdateEndpointIfTimeCriteriaIsNotMet() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789"
    )
    peer.verifyEndpoint(peer.endpoint!!, currentTime)
    assertEquals(currentTime, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)

    val endpoint = peer.endpoint
    val newEndpoint = Endpoint("127.0.0.1", 30303, 30303)
    peer.updateEndpoint(newEndpoint, currentTime + 10, currentTime)
    assertEquals(endpoint, peer.endpoint)
    assertEquals(currentTime, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)
  }

  @Test
  fun shouldUpdateEndpointIfTimeCriteriaIsMet() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789"
    )
    peer.verifyEndpoint(peer.endpoint!!, currentTime)
    assertEquals(currentTime, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)

    val newEndpoint = Endpoint("127.0.0.1", 30303, 30303)
    peer.updateEndpoint(newEndpoint, currentTime + 10, currentTime + 1)
    assertEquals(newEndpoint, peer.endpoint)
    assertEquals(currentTime + 10, peer.lastSeen)
    assertNull(peer.lastVerified)
  }

  @Test
  fun shouldUpdateLastSeenIfEndpointIsUnchanged() = runBlocking {
    val peer = peerRepository.get(
      "enode://c7849b663d12a2b5bf05b1ebf5810364f4870d5f1053fbd7500d38bc54" +
        "c705b453d7511ca8a4a86003d34d4c8ee0bbfcd387aa724f5b240b3ab4bbb994a1e09b@172.20.0.4:54789"
    )
    peer.verifyEndpoint(peer.endpoint!!, currentTime)
    assertEquals(currentTime, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)

    peer.verifyEndpoint(peer.endpoint!!, currentTime)
    peer.updateEndpoint(peer.endpoint!!, currentTime + 10, currentTime)
    assertEquals(currentTime + 10, peer.lastSeen)
    assertEquals(currentTime, peer.lastVerified)
  }
}