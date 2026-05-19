# Terraform — infraestructura cloud

Aquí van los módulos para provisionar:

- VPC + subredes públicas/privadas
- Cluster EKS / GKE / AKS
- RDS PostgreSQL 16 (Multi-AZ) con réplica de lectura
- ElastiCache Redis cluster
- Application Load Balancer + certificados ACM/cert-manager
- S3/GCS bucket para backups cifrados con KMS

> Los módulos no se incluyen en este commit inicial porque dependen del
> proveedor cloud escogido.  El backend está listo para correr en
> cualquier Kubernetes estándar — ver `../kubernetes/*.yaml`.
