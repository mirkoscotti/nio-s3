# NIO-S3 - Java NIO.2 implementation for AWS S3

## 🔭 Overview
This is a [JSR-203](https://jcp.org/en/jsr/detail?id=203) implementation of Amazon Simple Storage Service, a.k.a. S3, allowing operations on buckets as if they were file systems. This approach decouples the business logic from the underlying infrastructure chosen for I/O operations, making it straightforward to swap out the storage provider without affecting the rest of the codebase.

## Why another library
Throughout my experience as a Software Architect specializing in Java applications, I have often had to deal with dynamically managed file systems. In one particular project, a key requirement was to acquire files from external data providers through I/O operations using different protocols, such as SFTP and AWS S3 buckets. There were also cases involving migrations from one system to another.

To avoid rework whenever providers make infrastructure changes beyond the application’s control, I decided to adopt the Java NIO.2 standard.

In the specific case of Amazon S3, several libraries available on Maven Central implement the NIO.2 standard by exposing buckets as if they were as if they were fully-fledged file systems. However, after testing them, I realized that they did not cover a couple of essential use cases for our application workflow:

- The inability to handle file transfers between buckets owned by different accounts and accessed through different credentials.
- The inability to upload large files to a bucket without relying on the local file system to store temporary files before the upload process, resulting in performance issues.

To address these limitations, I ultimately chose the library that came closest to our requirements and forked it in order to support the missing use cases. This approach allowed us to achieve our objective, but I was never completely satisfied with the solution because, being based on a fork, I considered it inherently fragile.

For this reason, after leaving the company, I decided to develop my own solution — one that implements nearly 100% of the NIO.2 specification while fully supporting both of these critical use cases.

## 🚀 Quick Start
- Dipendenza Maven/Gradle
- Esempio minimo funzionante (3-5 righe di codice)

## ✨ Features / What's supported
- Tabella o checklist delle operazioni NIO.2 supportate (Files.copy, move, delete, walk, ecc.)
- Feature AWS SDK sfruttate (presigned URL, tagging, encryption, lifecycle, ecc.)
- Link a una matrice completa (in un file separato se lunga)

## ⚙️ Configuration
- Credential provider chain
- Region, endpoint, S3 client custom
- Proprietà specifiche (se presenti)

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