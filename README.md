# cosmic-pipeline

Generates and maintains external database identifiers linking active human genes
in RGD to the [COSMIC](https://cancer.sanger.ac.uk/cosmic) (Catalogue of Somatic
Mutations in Cancer) database.

## How It Works

Unlike most pipelines, this one does not import data from external files.
Instead, it derives COSMIC identifiers directly from the gene symbols of active
human genes already in RGD.

Each run performs three steps:

1. **Retrieve** — fetches all active human genes from RGD (excluding splice
   variants and alleles) and all existing COSMIC cross-references (XDB_KEY=45,
   SRC_PIPELINE=COSMIC).

2. **Compare** — performs a three-way set comparison between incoming and
   existing IDs to determine records to insert (new genes), delete (no longer
   active), and update (still active, modification date refreshed).

3. **Sync** — inserts new COSMIC references, deletes obsolete ones, and updates
   modification dates on matching records for audit tracking.

This incremental approach avoids drop-and-reload, preserving creation dates and
enabling change tracking over time.

## Output

For every active human gene, an `RGD_ACC_XDB` row is maintained with:

- `XDB_KEY` = 45 (COSMIC)
- `SRC_PIPELINE` = `COSMIC`
- `ACC_ID` and `LINK_TEXT` = the gene symbol
- `RGD_ID` = the gene's RGD ID

The COSMIC website renders a page for each gene symbol, so the symbol itself is
sufficient to construct the link.

## Build and run

Requires Java 17. Build with Gradle:

```
./gradlew clean assembleDist
```

Run via the produced distribution under `build/install/cosmic-pipeline/`.
Configuration lives in `properties/AppConfigure.xml`.
