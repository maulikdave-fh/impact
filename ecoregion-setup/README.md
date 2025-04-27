## Setup Instructions
In setup.js, point ```MONGODB_URL``` to your MongoDB instance. Make sure that the MongoDB database name is set to "earth".

**Important Notes** 
1. The previously loaded ecoregions will be purged.
2. ecoregion component won't work without this setup.

```bash
	mongosh --file setup.js
```

If all went well, you should have multiple documents under "ecoregion" collection.
