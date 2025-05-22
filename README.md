<img src="impact.png" alt="impact" width="15%"/>

Impact is open-source online platform to help Restoration Practitioners make data-driven decisions. 

It is intended to
1. improve "Plan -> Restore -> Observer -> Analyze" cycle.
2. provide meaningful insights about restoration projects to policy-makers, funding partners and clients.

## Context Map
```mermaid
    flowchart LR
        RP(Restoration Practitioner)
        BDI(Biodiversity Indexer)
        Species(Species)
        Observations(Observations)
        GBIFI(GBIFIntegrator)
        GBIF(GBIF)
        Ecoregion(Ecoregion)
        RS(Restoration Site)
        RFS(Reference Site)
        
        RP --> RS & RFS --> BDI
        BDI --> Species & Observations
        Species & Observations --> GBIFI --> GBIF
        GBIFI --> Ecoregion            
```


## Scope
### Functional
1. Restoration Practitioners (RP) can demarcate restoration sites in supported bioregions
2. Restoration Practitioners can demarcate reference sites in supported bioregions
3. Biodiversity Index - Derived for a restoration site in-context of the bioregion where the site belongs to
4. Restoration Site -> Reference Site(s) Comparator - RP can select reference site(s) and compare her restoration site index with that of reference site(s)

### Quality Requirements
#### Scalability
1. Millions of observations to be processed from Global Biodiversity Information Facility (GBIF) in the first fetch
2. Thousands of observations coming from GBIF(s) in subsequent fetches (depending on the fetch frequency & number of ecoregions supported)
3. Thousands of RPs - most RPs spending 1-2 hours/day

#### Availability
1. Aim for 99.9% up-time
2. Availability and Partition Tolerance to be prioritized over consistency

#### Performance
1. < 2secs of web-page load-time at 95th percentile
2. Less than 500ms response time 99p

### System Constraints
1. Avoid using cloud vendor specific tools / technologies - no vendor lock-in

## System's APIs
```mermaid
    sequenceDiagram
        participant RP as Restoration Practitioner
        participant Impact as Impact Platform
        participant GBIF
        Impact ->> GBIF : fetchObservations(eco-region coordinates)
        GBIF -->> +Impact : Observations
        Impact -->> -Impact : Store Observations, Update species
        RP ->> +Impact : sign_up(fullname, username, password, etc)
        Impact -->> -RP : Authentication Token + userId
        RP ->> +Impact : get_homepage(useId)
        Impact -->> -RP: Homepage + Restoration Sites, Reference Sites
        RP ->> +Impact : Create / View Restoration site / Reference site
        Impact -->> -RP : confirmation
        RP ->> +Impact : get_biodiversity_index(userId, siteId)
        Impact -->> Impact : Calculate Biodiversity Index
        Impact -->> -RP : biodiversity index
        RP ->> Impact : login(username, password)
        Impact -->> RP : auth token, userId
        RP ->> Impact : logout(userId)
        Impact -->> RP : confirmation             
```

### System APIs Listing
1. sign_up(fullname, username, password, email, profileimage) -> auth token, userId
2. login(username, password) -> auth token, userId
3. logout(userId) -> confirmation
4. get_homepage(userId) -> homepage + RP's restoration and reference sites
5. create_site(userId, polygon, siteType) -> siteId. siteType - restoration | reference
6. update_site(userId, siteId, polygon, siteType) -> siteId
7. delete_site(userId, siteId) -> confirmation
8. get_biodiversity_index(userId, siteId) -> biodiversity index
9. get_habitat_indicator(userId, siteId) -> habitat indicator

## System Architecture
![System Architecture!](System_Architecture-IMPACT___System_Architecture__V0_1_.png)

### Notes
1. Observations and Species data sets should be sharded by bioregion.
2. Every microservice and database to be replicated.
3. The system to run in multiple data centers / regions - 1 data center per realm
4. Following components are expected to be part of the system, but not highlighted in the system architecture
- Distributed Logging & Monitoring
- Service Registry and Discovery

## Roadmap
- [ ] Habitat Indicator -> indicates how a given restoration site is evolving over-time in-terms of habitat niches
- [ ] Stakeholder Access
- [ ] Ability to manage observations. In current version, RPs have to manage their observations at the supported Crowd-sourced Species Identification Systems (CSIS)
- [ ] Contextual Ads - Native Plant Nurseries, Restoration services, Geo survey services, Permaculture design services, etc.
- [ ] Native Mobile App Support
- [ ] Discussion Forum
