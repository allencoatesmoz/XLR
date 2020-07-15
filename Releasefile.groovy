// Exported from:        http://ccy.local:5952/#/templates/FolderSamplesAndTutorials-Release675ef08b84fe47a9acc322dc8a5c16a5/releasefile
// XL Release version:   9.5.2
// Date created:         Thu Mar 12 16:30:23 CET 2020

xlr {
  template('ECommerce Microservices Delivery') {
    folder('Samples & Tutorials')
    variables {
      stringVariable('DeliveryID') {
        required false
        showOnReleaseStart false
      }
      stringVariable('DeliveryName') {
        label 'Name of the new delivery'
      }
      mapVariable('JiraTickets') {
        required false
        showOnReleaseStart false
      }
      listVariable('TrackedItems') {
        required false
        showOnReleaseStart false
        label 'Jira tickets translated to tracked items for the delivery'
      }
      mapVariable('ServiceNowData') {
        required false
        showOnReleaseStart false
      }
      stringVariable('ServiceNowSysId') {
        required false
        showOnReleaseStart false
      }
      stringVariable('ServiceNowNumber') {
        required false
        showOnReleaseStart false
      }
    }
    scheduledStartDate Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", '2020-03-12T09:00:00+0100')
    tags 'ecommerce', 'delivery'
    scriptUsername 'robot'
    scriptUserPassword '{aes:v0}oa/ze0iG73VKutoZn2C8FdJAdQcxmWHmtEAIZCp3rdg='
    phases {
      phase('Create delivery') {
        color '#00875A'
        tasks {
          custom('Create weekly delivery') {
            team 'Release Managers'
            script {
              type 'delivery.CreateDelivery'
              title '${DeliveryName}'
              deliveryId variable('DeliveryID')
            }
          }
          custom('Get Jira tickets for all microservices in the delivery') {
            team 'Release Managers'
            script {
              type 'jira.Query'
              query 'project = SAN AND issuetype in (Bug, Story) AND creator in (ajohnston) AND labels = SoftwareDeliveryExample'
              issues variable('JiraTickets')
            }
          }
          script('Parse Jira tickets') {
            team 'Release Managers'
            script (['''\
TrackedItems = []

for k, v in releaseVariables['JiraTickets'].items():
    TrackedItems.append('{} {}'.format(k, v))

releaseVariables['TrackedItems'] = TrackedItems
'''])
          }
          custom('Register Jira tickets as tracked items in delivery') {
            team 'Release Managers'
            script {
              type 'delivery.RegisterTrackedItems'
              deliveryId '${DeliveryID}'
              trackedItems variable('TrackedItems')
            }
          }
        }
      }
      phase('Wait for CI cycles') {
        color '#CC4A3C'
        tasks {
          custom('Wait until end-to-end testing is finished') {
            team 'Release Managers'
            script {
              type 'delivery.WaitForStage'
              deliveryId '${DeliveryID}'
            }
          }
          notification('Send update to Product Management') {
            team 'Release Managers'
            subject 'Delivery ${DeliveryName} has reached E2E Testing'
            body 'Delivery ${DeliveryName} has reached E2E Testing'
          }
        }
      }
      phase('Approval') {
        color '#FFAB00'
        tasks {
          custom('Create change request for production deployment') {
            team 'Ops'
            script {
              type 'servicenow.CreateChangeRequest'
              data variable('ServiceNowData')
              shortDescription 'Deployment for ${DeliveryName}'
              description 'Change request for delivery ${DeliveryName} (ID ${DeliveryID}), triggered from ${release.title}'
              assignmentGroup 'CAB Approval'
              state 'Assess'
              priority '2 - High'
              sysId variable('ServiceNowSysId')
              'Ticket' variable('ServiceNowNumber')
            }
          }
          custom('Wait until all tracked items are deployed to production') {
            team 'Release Managers'
            script {
              type 'delivery.WaitForStage'
              deliveryId '${DeliveryID}'
            }
          }
          custom('Close change request') {
            team 'Ops'
            script {
              type 'servicenow.UpdateChangeRequest'
              data variable('ServiceNowData')
              assignmentGroup 'Change Management'
              state 'Closed'
              'Ticket' variable('ServiceNowNumber')
              shortDescription 'Deployment for ${DeliveryName}'
              sysId '${ServiceNowSysId}'
              closeCode 'successful'
              closeNotes 'Delivery ${DeliveryName}, ID ${DeliveryID}'
            }
          }
        }
      }
    }
    
  }
}