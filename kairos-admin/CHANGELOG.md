# Changelog

## [0.3.0](https://github.com/kekukhvy/kairos/compare/kairos-admin-v0.2.0...kairos-admin-v0.3.0) (2026-07-13)


### Features

* **admin:** add guided setup wizard (Task → Destination → Schedule) ([f0cba7f](https://github.com/kekukhvy/kairos/commit/f0cba7f5bc68368b5d0d1624bd0868ba5b9e343d)), closes [#23](https://github.com/kekukhvy/kairos/issues/23)
* **admin:** add guided setup wizard to admin UI ([014b27e](https://github.com/kekukhvy/kairos/commit/014b27e247e161d4f6a06b8625f1c6faded4072c))
* **admin:** open destination grid details on single click ([5ed7a68](https://github.com/kekukhvy/kairos/commit/5ed7a6872d2d7ae6ca8afe53bef11d68f83ab91b)), closes [#33](https://github.com/kekukhvy/kairos/issues/33)
* **admin:** share cron builder in wizard, add requireJson helper ([8047f8a](https://github.com/kekukhvy/kairos/commit/8047f8a1281406cb491a8383c43f8e6833352401))
* **admin:** unify Task & Schedule grids — single-click row, in-row toggle switch ([6b8fbfe](https://github.com/kekukhvy/kairos/commit/6b8fbfee6877510c7232f9956952dd3f212785fe))
* **admin:** unify Task & Schedule grids — single-click row, in-row toggle switch ([36c636c](https://github.com/kekukhvy/kairos/commit/36c636c0de71359e8aafd1ca9a208ea749a4a448)), closes [#26](https://github.com/kekukhvy/kairos/issues/26)

## [0.2.0](https://github.com/kekukhvy/kairos/compare/kairos-admin-v0.1.0...kairos-admin-v0.2.0) (2026-07-12)


### Features

* **admin:** add Schedule management tab to admin UI ([8f3ef1d](https://github.com/kekukhvy/kairos/commit/8f3ef1db6d46bccfda982186b6db0dfc4774d7c7))
* **admin:** Dashboard home landing page with customizable card grid ([e883149](https://github.com/kekukhvy/kairos/commit/e88314928ca2b00674fa363f564b50c8f3ecbba3))
* **admin:** visual CRON expression builder + demo data seeder ([6176a1e](https://github.com/kekukhvy/kairos/commit/6176a1e464f69b7b858579e30ac63b9e8b835b53))
* **api:** add connection and read timeout properties to ApiProperties and update client configuration ([2589a9c](https://github.com/kekukhvy/kairos/commit/2589a9c36d1e5c763ac2bd03be6b20a867b8a3cf))
* **dashboard:** add dashboard navigation item to admin UI ([19068e3](https://github.com/kekukhvy/kairos/commit/19068e31f3c039f11d4753079d2a37703304e032))
* **dashboard:** customizable dashboard home landing page + tests ([af579d8](https://github.com/kekukhvy/kairos/commit/af579d8aa8fa733b972278a1c1310861e6ff8922)), closes [#24](https://github.com/kekukhvy/kairos/issues/24)
* **date:** add DateTimes utility for formatting timestamps in admin UI ([53b2ee1](https://github.com/kekukhvy/kairos/commit/53b2ee17691b62d309eaa5de6d5f588670563c44))
* **destination, task:** add filter functionality and UI enhancements for destination and task grids ([10d4896](https://github.com/kekukhvy/kairos/commit/10d4896cfd552f45c325a6f12dd3562db1fe502c))
* **destination:** add CreateDestinationRequest and DestinationForm for creating destinations in admin UI ([870e798](https://github.com/kekukhvy/kairos/commit/870e798d8f45da27fa6b2ee4b289899526d18801))
* **destination:** add DestinationDTO, DestinationPage, and DestinationGrid for admin UI; update ApiProperties and MainLayout ([6b0968c](https://github.com/kekukhvy/kairos/commit/6b0968cfb1242ca50e8612448bf510c0b18cc5eb))
* **destination:** add getById method to fetch destination details and enhance TaskGrid with clickable destination links ([e446443](https://github.com/kekukhvy/kairos/commit/e446443e1c17ae03f4d76f9c9e50ddb7b6e928e9))
* **destination:** implement full CRUD functionality for destination management in admin UI ([4cea8e8](https://github.com/kekukhvy/kairos/commit/4cea8e8d63bfa03aef73d64585efea82e3f357ec))
* **destination:** refactor destination details and validation logic for improved clarity and maintainability ([6a09ae9](https://github.com/kekukhvy/kairos/commit/6a09ae9236d48bdfae3e322e072db393c5cc1910))
* **schedule:** add CSS constants and demo data seeder for schedule management ([6476f95](https://github.com/kekukhvy/kairos/commit/6476f95629f48493b57ecdf01da97ce449d0274a))
* **schedule:** add schedule management tab and related UI components to admin interface ([2994884](https://github.com/kekukhvy/kairos/commit/29948842f07d9e6b2eb8ac3173a3f5b09772d8d4))
* **schedule:** add visual CRON expression builder to ScheduleForm and related components ([61815fa](https://github.com/kekukhvy/kairos/commit/61815fa7ea7be2b2f19c6aebd08c81a3bdc4a63c))
* **schedule:** enhance schedule management with timezone support and pagination for task schedules ([576ade0](https://github.com/kekukhvy/kairos/commit/576ade04430b09ec99bb9e71b425f38bd5901660))
* **schedule:** update demo data seeder to include multiple schedules for a single task ([e70db7b](https://github.com/kekukhvy/kairos/commit/e70db7bdee99b1c7935c91b07392a614dbf40070))
* **task:** add task action buttons and functionality for viewing, editing, activating, and deleting tasks ([f2813a5](https://github.com/kekukhvy/kairos/commit/f2813a52d0af8799719143d623c23acf69d1145b))
* **task:** add task details dialog and update task form layout ([82946f6](https://github.com/kekukhvy/kairos/commit/82946f654015e80af35037af731d42c908d07855))
* **task:** enhance task form with edit functionality and add badge support ([03b2915](https://github.com/kekukhvy/kairos/commit/03b2915aa233fbfb3e5b2b05fbe55b835f67c094))
* **task:** implement task management UI and API integration ([0d264d6](https://github.com/kekukhvy/kairos/commit/0d264d607b7c9328c5be7642b6faeae365e049e2))
* **task:** integrate searchable ComboBox for destination selection in TaskForm ([8f97db2](https://github.com/kekukhvy/kairos/commit/8f97db254b1e185b5d9565446c88fbf4bef1d9e5))
* **task:** rename messageType to eventName for improved clarity in task handling ([e9e35f3](https://github.com/kekukhvy/kairos/commit/e9e35f33d644923c565291f99be6bc1b06cf0847))
* **ui:** add shared UI components and styling for admin interface ([81e14fe](https://github.com/kekukhvy/kairos/commit/81e14fe9a7707c81d60321a8c05eaf653895d310))
* **ui:** scaffold admin UI with Vaadin components and configuration ([fdfca80](https://github.com/kekukhvy/kairos/commit/fdfca80a5c948101247513bbd367d31bfa40b649))
