# cosmic-pipeline

Generates and maintains external database identifiers linking active human genes in RGD to the [COSMIC](https://cancer.sanger.ac.uk/cosmic) (Catalogue of Somatic Mutations in Cancer) database.

## How It Works

Unlike most pipelines, this does not import data from external files. Instead, it derives COSMIC identifiers directly from gene symbols of active human genes already in RGD.

1. **Retrieve** — Fetches all active human genes from RGD (excluding splice variants and alleles) and all existing COSMIC cross-references (XDB_KEY=45).

2. **Compare** — Performs a three-way set comparison between incoming and existing IDs to determine records to insert (new genes), delete (no longer active), and update (still active).

3. **Sync** — Inserts new COSMIC references, deletes obsolete ones, and updates modification dates on matching records for audit tracking.

This incremental approach avoids drop-and-reload, preserving creation dates and enabling change tracking over time.
