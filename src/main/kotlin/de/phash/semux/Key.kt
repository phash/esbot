package de.phash.semux

import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.KeyPairGenerator
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class Key {
    private val gen = KeyPairGenerator()
    var sk: EdDSAPrivateKey
    var pk: EdDSAPublicKey

    /**
     * Creates a random ED25519 key pair.
     */
    constructor() {
        val keypair = gen.generateKeyPair()
        sk = keypair.private as EdDSAPrivateKey
        pk = keypair.public as EdDSAPublicKey
    }

    /**
     * Creates an ED25519 key pair with a specified private key
     *
     * @param privateKey
     * the private key in "PKCS#8" format
     * @throws InvalidKeySpecException
     */
    @Throws(InvalidKeySpecException::class)
    constructor(privateKey: ByteArray) {
        this.sk = EdDSAPrivateKey(PKCS8EncodedKeySpec(privateKey))
        this.pk = EdDSAPublicKey(EdDSAPublicKeySpec(sk.a, sk.params))
    }

    /**
     * Creates an ED25519 key pair with the specified public and private keys.
     *
     * @param privateKey
     * the private key in "PKCS#8" format
     * @param publicKey
     * the public key in "X.509" format, for verification purpose only
     *
     * @throws InvalidKeySpecException
     */
    @Throws(InvalidKeySpecException::class)
    constructor(privateKey: ByteArray, publicKey: ByteArray) {
        this.sk = EdDSAPrivateKey(PKCS8EncodedKeySpec(privateKey))
        this.pk = EdDSAPublicKey(EdDSAPublicKeySpec(sk.a, sk.params))

        if (!Arrays.equals(getPublicKey(), publicKey)) {
            throw InvalidKeySpecException("Public key and private key do not match!")
        }
    }

    fun generate() {
        val gen = KeyPairGenerator()
        val keyPair = gen.generateKeyPair()

    }


    /**
     * Returns the private key, encoded in "PKCS#8".
     */
    fun getPrivateKey(): ByteArray {
        return sk.encoded
    }

    /**
     * Returns the public key, encoded in "X.509".
     *
     * @return
     */
    fun getPublicKey(): ByteArray {
        return pk.encoded
    }

    /**
     * Returns the Semux address.
     */
    fun toAddress(): ByteArray {
        return Hash.h160(getPublicKey())
    }

    /**
     * Returns the Semux address in [String].
     */
    fun toAddressString(): String {
        return Hex.encode(toAddress())
    }


}