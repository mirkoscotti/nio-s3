# NIO-S3 - Java NIO.2 implementation for AWS S3

## 🔭 Overview
This is a [JSR-203](https://jcp.org/en/jsr/detail?id=203) implementation of Amazon Simple Storage Service, a.k.a. S3, allowing operations on buckets as if they were file systems. This approach decouples the business logic from the underlying infrastructure chosen for I/O operations, making it straightforward to swap out the storage provider without affecting the rest of the codebase.

## 🤷‍♂️ Why Another Library
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

## 📐 Design Decisions
### FileSystemProvider
It is a container of bucket descriptors and credentials. Rules:
- Many buckets can be accessed with the same credentials
- A bucket cannot be accessed by different credentials

### FileSystem
Buckets are mapped to file system instances. Rules:
- Naming convention according to the [AWS specification](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html)
- A bucket must physically exist. When FileSystems#newFileSystem is invoked, the bucket is created if credentials allow it and it does not exist
- An existing bucket whose descriptor still has not been cached by the `FileSystemProvider` it is considered *new*. According to the specification:
  - `FileSystems#newFileSystem` does not throw an exception
  - `FileSystems#getFileSystem` throws `FileSystemNotFoundException`
  - The AWS `BucketAlreadyOwnedByYouException` is ignored
  - The `BucketAlreadyExistsException` is converted to `AccessDeniedException`, not `FileSystemAlreadyExistsException`
- An existing bucket whose descriptor has already been cached by the `FileSystemProvider` it is considered *existing*. According to the specification:
  - `FileSystems#newFileSystem` throws `FileSystemAlreadyExistsException`
  - `FileSystems#getFileSystem` does not throw an exception

### Path
Bucket objects are mapped to path instances. Rules:
- Naming convention according to [AWS specification](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html).
- All warnings in the AWS documentation correspond to restrctions applied to the object key.
- Although keys starting with `/` are not allowed by AWS, this character is permitted here to distinguish absolute paths from relative ones.
- The key `/` is considered the only root path in the file system.
- Keys ending with `/` identify directories
- POSIX relative paths like `.` and `..` are interpreted according to their usual meaning. The following object keys are considered equivalent:
  - `abc/xyz/../file.txt`
  - `abc/./file.txt`
  - `abc/file.txt`
- Keys where the number of `..` exceeds the number of preceeding `/` (excluding the leading `/` that marks an absolute path) are forbidden

### SeekableByteChannel
File streaming does not fully cover the NIO.2 specifications. Limitations:
- A channel cannot be opened for both reading and writing
- Positioning is available only when a channel is open for read
- Size is available only when channel is open for read
- Truncations are not yet supported.

In all other use cases, the behaviour of the channel implementation is exactly the same as that of the default file system.

### Less frequently used components
- **FileStore** - Collects bucket metadata. Each bucket defines a single file store
- **BasicFileAttributes** - Collects object metadata when available from AWS S3 service. Differences from the NIO.2 specifications:
  - *last access time* - Not available, falls back to to *last modified time*
  - *creation time* - Not available, falls back to to *last modified time*
  - *symbolic link* - Not supported by AWS S3, always returns `false`
- **BasicFileAttributeView** - Supported only for reading metadata. It is not possible to modify them because because AWS S3 does not support it
- **DirectoryStream** - 
- **WatchService** - 
  

## 📚 Usage examples
- Esempi progressivi: base → avanzato
- Focus sui casi d'uso che le altre librerie non coprono

## ⚠️ Limitations / Not supported
- Onestà intellettuale: cosa *non* è supportato (es. watch service, file lock, ecc.)
- Aiuta a gestire le aspettative e riduce issue inutili

## 📄 License