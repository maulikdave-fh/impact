const fs = require('fs');

const MONGODB_URL = 'mongodb://127.0.0.1:27017/earth';

db = connect(MONGODB_URL);
db.ecoregion.drop();
console.log('Setting-up eco-regions....');
let jsonFilePath = "./ecoregions.json";
let data = fs.readFileSync(jsonFilePath, 'utf8');
const jsonData = JSON.parse(data);

let count = 0;
jsonData.forEach(ecoRegion => {
    let ecoregionMapData = fs.readFileSync(ecoRegion["geoJSON"], "utf8");
    const ecoregionMapJSONData = JSON.parse(ecoregionMapData)["features"][0]["geometry"];
    ecoRegion["regionMap"] = ecoregionMapJSONData;
    delete ecoRegion["geoJSON"];
    count++;
});

db.ecoregion.insertMany(jsonData);
db.ecoregion.createIndex({regionMap : "2dsphere"});
console.log(`DONE: Inserted ${count} ecoregions!`);