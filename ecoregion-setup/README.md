## Setup Instructions
In setup.js, point ```MONGODB_URL``` to your MongoDB instance. Make sure that the MongoDB database name is set to "earth".

```bash
	mongosh --file setup.js
```

If all went well, you should have multiple documents under "ecoregion" collection.
