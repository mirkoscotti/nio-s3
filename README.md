# NIO-S3 - Java NIO.2 implementation for AWS S3

## 🔭 Overview
This is a [JSR-203](https://jcp.org/en/jsr/detail?id=203) implementation of Amazon Simple Storage Service, a.k.a. S3, allowing operations on buckets as if they were file systems. This approach decouples the business logic from the underlying infrastructure chosen for I/O operations, making it straightforward to swap out the storage provider without affecting the rest of the codebase.

## 🤷‍♂️ Why another library
Throughout my experience as a Software Architect specializing in Java applications, I have often had to deal with dynamically managed file systems. In one particular project, a key requirement was to acquire files from external data providers through I/O operations using different protocols, such as SFTP and AWS S3 buckets. There were also cases involving migrations from one system to another.

To avoid rework whenever providers make infrastructure changes beyond the application’s control, I decided to adopt the Java NIO.2 standard.

In the specific case of Amazon S3, several libraries available on Maven Central implement the NIO.2 standard by exposing buckets as if they were fully-fledged file systems. However, after testing them, I realized that they did not cover a couple of critical use cases for our application workflow:

- The inability to handle file transfers between buckets owned by different accounts and accessed through different credentials.
- The inability to upload large datasets to a bucket without relying on the local file system to store temporary files before the upload process, resulting in performance issues.

To address these limitations, I ultimately chose the library that came closest to our requirements and forked it in order to support the missing use cases. This approach allowed us to achieve our objective, but I was never completely satisfied with the solution because, being based on a fork, I considered it inherently fragile.

For this reason, after leaving the company, I decided to develop my own solution — one that implements nearly 100% of the NIO.2 specification while fully supporting both of these use cases.

## 🚀 Quick Start

### Installation

#### Java compatibility
| Component | Version |
|---|---|
| JDK | 21+ |
| AWS SDK | 2.44.4 |

#### Maven
```
<dependency>
    <groupId>io.github.mirkoscotti</groupId>
    <artifactId>nio-s3</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```
implementation 'io.github.mirkoscotti:nio-s3:1.0.0'
```

### ⚙️ Configuration

#### Supported URIs

| Style | URI | Notes |
|---|---|---|
| Bucket style | `s3://bucket-name` | |
| Virtual host style | `s3://bucket-name.s3.region-code.amazonaws` | |
| Path style | `s3://custom-endpoint/bucket-name` | Also for testing purpose (i.e. Localstack)|
| Credentials style | `s3://access-key:secret-key@one-of-the-previous-style` | Mandatory for `Paths.get(uri)` and not existing file system|

#### Base properties
- ✅ Mandatory
- ⬜ Optional

| Property | Bucket style | Virtual host style | Path style | Credentials style | Default |
|---|:---:|:---:|:---:|:---:|---|
| `aws.endpoint` | ⬜ | ⬜ | ✅ | ⬜ | `https://s3.us-east-1.amazonaws.com` |
| `aws.region` | ✅ | ⬜ |✅ | ⬜ | `us-east-1` |
| `aws.access-key` | ✅ | ✅ | ✅ | ⬜ | |
| `aws.secret-key` | ✅ | ✅ | ✅ | ⬜ | |

#### Additional optional properties
The following properties are supported only when the creation of a new file system results in the creation of a new bucket. See [Configuring ACLs](https://www.esempio.com) for further details.

| Property |
|---|
| `aws.acl` |
| `aws.full-control` |
| `aws.read` |
| `aws.read-acp` |
| `aws.write` |
| `aws.write-acp` |

## ✨ Features / What's supported
- Tabella o checklist delle operazioni NIO.2 supportate (Files.copy, move, delete, walk, ecc.)
- Feature AWS SDK sfruttate (presigned URL, tagging, encryption, lifecycle, ecc.)
- Link a una matrice completa (in un file separato se lunga)

## 📚 Usage examples
- Esempi progressivi: base → avanzato
- Focus sui casi d'uso che le altre librerie non coprono

## 🧭 Compatibility
- Versioni Java supportate
- Versioni AWS SDK v2.x testate
- Note su backward compatibility

## ⚠️ Limitations / Not supported
- Onestà intellettuale: cosa *non* è supportato (es. watch service, file lock, ecc.)
- Aiuta a gestire le aspettative e riduce issue inutili

## 🔗 References
- Java NIO.2 SPI docs
- AWS SDK S3 documentation
- Link a ARCHITECTURE.md / CONTRIBUTING.md

## 🤝 Contributing (opzionale)
- Solo se vuoi contributi esterni: linee guida minime

## 📄 License