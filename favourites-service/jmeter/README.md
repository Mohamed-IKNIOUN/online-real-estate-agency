# JMeter pour `favourites-service`

Le plan nécessite Apache JMeter 5.6.3 et une instance locale isolée de `favourites-service` reliée à des services et à des données de test. Utilisez uniquement une adresse de bouclage qui dessert cette instance. Un tunnel vers un environnement partagé ou de production est exclu.

Le lanceur exécute d’abord un scénario GET authentifié et vérifie HTTP 200 ainsi qu’un tableau JSON. Il vérifie ensuite deux requêtes distinctes, exécutées une seule fois chacune. Un GET sans jeton doit renvoyer 401 et un GET pour un autre client, avec le même jeton valide, doit renvoyer 403. Les contrôles négatifs ont leur propre fichier JTL et sont exclus des statistiques de charge. Le profil complet enchaîne ensuite des paliers à 5, 20 et 50 utilisateurs virtuels. Toutes les requêtes utilisent GET.

## Jeton de test

Connectez-vous au service d’identité de l’environnement isolé avec un compte de test ayant le rôle `BUYER`. Récupérez un jeton d’accès de courte durée, avec une durée suffisante pour les scénarios choisis. Renseignez l’identifiant numérique du compte comme `ClientId` et l’identifiant d’un autre compte de test comme `OtherClientId`. Le plan vérifie localement le format du jeton, l’algorithme HS256, le rôle, l’expiration et la correspondance exacte entre `sub` et `ClientId`. Le service valide la signature lors du scénario initial.

Depuis le répertoire `favourites-service/jmeter`, dans Windows PowerShell 5.1 ou PowerShell 7, adaptez le chemin de JMeter, le port et les deux identifiants. Collez le jeton à l’invite masquée. Le lanceur transmet le jeton par l’environnement du processus enfant. N’activez pas la trace HTTP ou les collecteurs qui enregistrent les en-têtes.

```powershell
$jmeter = 'C:\tools\apache-jmeter-5.6.3\bin\jmeter.bat'
$clientId = 101
$otherClientId = 102

$secureJwt = Read-Host 'Jeton d’accès BUYER de test' -AsSecureString
try {
    $buffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureJwt)
    try {
        $env:JWT_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($buffer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($buffer)
    }
    .\run-local.ps1 -JMeter $jmeter -TargetHost 127.0.0.1 -Port 8083 `
        -ClientId $clientId -OtherClientId $otherClientId -SmokeOnly
} finally {
    Remove-Item Env:JWT_TOKEN -ErrorAction SilentlyContinue
    $secureJwt.Dispose()
}
```

Pour exécuter aussi les paliers de charge, relancez le bloc avec un jeton valide et retirez `-SmokeOnly`. Les paramètres JMeter correspondants sont `HOST`, `PORT`, `CLIENT_ID` et `OTHER_CLIENT_ID`. Le lanceur configure les journaux et fichiers JTL pour exclure les en-têtes, les données de requête et les corps de réponse. Il écrit les résultats sous `results/`, ignoré par Git.
